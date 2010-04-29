package OBM::EntitiesFactory::hostFactory;

$VERSION = '1.0';

use OBM::EntitiesFactory::factory;
@ISA = ('OBM::EntitiesFactory::factory');

$debug = 1;

use 5.006_001;
require Exporter;
use strict;

use OBM::Tools::commonMethods qw(
        _log
        dump
        );
use OBM::Parameters::regexp;


sub new {
    my $class = shift;
    my( $updateType, $parentDomain, $ids ) = @_;

    my $self = bless { }, $class;

    $self->{'updateType'} = $updateType;
    if( !$self->_checkUpdateType() ) {
        return undef;
    }

    if( !defined($parentDomain) ) {
        $self->_log( 'description du domaine père indéfini', 3 );
        return undef;
    }

    if( ref($parentDomain) ne 'OBM::Entities::obmDomain' ) {
        $self->_log( 'description du domaine père incorrecte', 3 );
        return undef;
    }
    $self->{'parentDomain'} = $parentDomain;
    
    $self->{'domainId'} = $parentDomain->getId();
    if( ref($self->{'domainId'}) || ($self->{'domainId'} !~ /$regexp_id/) ) {
        $self->_log( 'identifiant de domaine \''.$self->{'domainId'}.'\' incorrect', 3 );
        return undef;
    }

    if( defined($ids) && (ref($ids) ne 'ARRAY') ) {
        $self->_log( 'liste d\'ID à traiter incorrecte', 3 );
        return undef;
    }

    if( $#{$ids} >= 0 ) {
        $self->{'ids'} = $ids;
    }

    $self->{'running'} = undef;
    $self->{'currentEntity'} = undef;
    $self->{'entitiesDescList'} = undef;


    return $self;
}


sub next {
    my $self = shift;

    $self->_log( 'obtention de l\'entité suivante', 3 );

    if( !$self->isRunning() ) {
        if( !$self->_start() ) {
            $self->_reset();
            return undef;
        }
    }

    while( defined($self->{'entitiesDescList'}) && (my $userHostDesc = $self->{'entitiesDescList'}->fetchrow_hashref()) ) {
        require OBM::Entities::obmHost;
        if( !(my $current = OBM::Entities::obmHost->new( $self->{'parentDomain'}, $userHostDesc )) ) {
            next;
        }else {
            $self->{'currentEntity'} = $current;
            if( $self->_getSambaServiceState() ) {
                $self->_log( 'probleme au chargement du status Samba de l\'entité '.$self->{'currentEntity'}->getDescription(), 1 );
                next;
            }

            SWITCH: {
                if( $self->{'updateType'} eq 'UPDATE_ALL' ) {
                    if( $self->_loadHostLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 2 );
                        next;
                    }

                    $self->_log( 'mise à jour de l\'entité, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setUpdateEntity();
                    $self->{'currentEntity'}->setUpdateLinks();
                    last SWITCH;
                }

                if( $self->{'updateType'} eq 'UPDATE_ENTITY' ) {
                    $self->_log( 'mise à jour de l\'entité, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setUpdateEntity();
                    last SWITCH;
                }

                if( $self->{'updateType'} eq 'UPDATE_LINKS' ) {
                    if( $self->_loadHostLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 2 );
                        next;
                    }

                    $self->_log( 'mise à jour des liens, '.$self->{'currentEntity'}->getDescription(), 3 );

                    $self->{'currentEntity'}->setUpdateLinks();
                    last SWITCH;
                }

                if( $self->{'updateType'} eq 'DELETE' ) {
                    $self->_log( 'suppression de l\'entité, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setDelete();
                    last SWITCH;
                }

                $self->_log( 'type de mise à jour inconnu \''.$self->{'updateType'}.'\'', 0 );
                return undef;
            }

            return $self->{'currentEntity'};
        }
    }

    $self->{'currentEntity'} = undef;

    return undef;
}


sub _loadEntities {
    my $self = shift;

    $self->_log( 'chargement des hôtes du domaine d\'identifiant \''.$self->{'domainId'}.'\'', 2 );

    require OBM::Tools::obmDbHandler;
    my $dbHandler = OBM::Tools::obmDbHandler->instance();

    if( !$dbHandler ) {
        $self->_log( 'connexion à la base de données impossible', 4 );
        return 1;
    }

    my $hostTablePrefix = '';
    if( $self->{'updateType'} !~ /^(UPDATE_ALL|UPDATE_ENTITY)$/ ) {
        $hostTablePrefix = 'P_';
    }

    my $query = 'SELECT '.$hostTablePrefix.'Host.*,
                        current.host_name as host_name_current
                 FROM '.$hostTablePrefix.'Host
                 LEFT JOIN P_Host current ON current.host_id='.$hostTablePrefix.'Host.host_id
                 WHERE '.$hostTablePrefix.'Host.host_domain_id='.$self->{'domainId'};

    if( $self->{'ids'} ) {
        $query .= ' AND '.$hostTablePrefix.'Host.host_id IN ('.join( ', ', @{$self->{'ids'}} ).')';
    }

    $query .= ' ORDER BY '.$hostTablePrefix.'Host.host_name';

    if( !defined($dbHandler->execQuery( $query, \$self->{'entitiesDescList'} )) ) {
        $self->_log( 'chargement des hôtes depuis la BD impossible', 3 );
        return 1;
    }

    return 0;
}


sub _loadHostLinks {
    my $self = shift;

    $self->_log( 'chargement des liens de '.$self->{'currentEntity'}->getDescription(), 2 );

    return $self->_getSambaServiceState();
}


sub _getSambaServiceState {
    my $self = shift;

    $self->_log( 'obtention du status Samba de '.$self->{'currentEntity'}->getDescription(), 3 );

    require OBM::Tools::obmDbHandler;
    my $dbHandler = OBM::Tools::obmDbHandler->instance();

    if( !$dbHandler ) {
        $self->_log( 'connexion à la base de données impossible', 4 );
        return 1;
    }

    my $entityId = $self->{'currentEntity'}->getId();

    my $hostEntityTable = 'HostEntity';
    my $serviceTable = 'Service';
    if( $self->{'updateType'} =~ /^(SYSTEM_ALL|SYSTEM_ENTITY|SYSTEM_LINKS)$/ ) {
        $hostEntityTable = 'P_'.$hostEntityTable;
        $serviceTable = 'P_'.$serviceTable;
    }

    my $query = 'SELECT service_id AS host_samba
                 FROM '.$serviceTable.'
                 INNER JOIN '.$hostEntityTable.' ON hostentity_host_id='.$entityId.'
                 WHERE service_entity_id=hostentity_entity_id AND service_service=\'samba\'
                 LIMIT 1';

    my $queryResult;
    if( !defined($dbHandler->execQuery( $query, \$queryResult )) ) {
        $self->_log( 'chargement des liens de '.$self->{'currentEntity'}->getDescription().' depuis la BD impossible', 3 );
        return 1;
    }

    my $links = $queryResult->fetchrow_hashref();
    $queryResult->finish();

    $self->{'currentEntity'}->setLinks( $links );

    return 0;
}
