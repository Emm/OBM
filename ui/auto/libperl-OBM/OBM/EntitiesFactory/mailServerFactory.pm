package OBM::EntitiesFactory::mailServerFactory;

$VERSION = '1.0';

use OBM::EntitiesFactory::factory;
use OBM::Log::log;
@ISA = ('OBM::EntitiesFactory::factory', 'OBM::Log::log');

$debug = 1;

use 5.006_001;
require Exporter;
use strict;

use OBM::Parameters::regexp;


sub new {
    my $class = shift;
    my( $updateType, $parentDomain ) = @_;

    my $self = bless { }, $class;

    $self->{'updateType'} = $updateType;
    if( !$self->_checkUpdateType() ) {
        return undef;
    }

    if( !defined($parentDomain) ) {
        $self->_log( 'description du domaine père indéfini', 1 );
        return undef;
    }

    if( ref($parentDomain) ne 'OBM::Entities::obmDomain' ) {
        $self->_log( 'description du domaine père incorrecte', 1 );
        return undef;
    }
    $self->{'parentDomain'} = $parentDomain;
    
    $self->{'domainId'} = $parentDomain->getId();
    if( ref($self->{'domainId'}) || ($self->{'domainId'} !~ /$regexp_id/) ) {
        $self->_log( 'identifiant de domaine \''.$self->{'domainId'}.'\' incorrect', 1 );
        return undef;
    }

    $self->{'running'} = undef;
    $self->{'currentEntity'} = undef;
    $self->{'entitiesDescList'} = undef;


    return $self;
}


sub next {
    my $self = shift;

    $self->_log( 'obtention de l\'entité suivante', 4 );

    if( !$self->isRunning() ) {
        if( !$self->_start() ) {
            $self->_reset();
            return undef;
        }
    }

    if( defined($self->{'entitiesDescList'}) && (my $mailServerDesc = $self->{'entitiesDescList'}->fetchall_arrayref({})) ) {
        require OBM::Entities::obmMailServer;
        if( $self->{'currentEntity'} = OBM::Entities::obmMailServer->new( $self->{'parentDomain'}, $mailServerDesc ) ) {
            SWITCH: {
                if( $self->{'updateType'} eq 'UPDATE_ALL' ) {
                    if( $self->_loadMailServerLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 1 );
                        return undef;
                    }

                    $self->_log( 'mise à jour de l\'entité et des liens, '.$self->{'currentEntity'}->getDescription(), 3 );
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
                    if( $self->_loadMailServerLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 1 );
                        return undef;
                    }

                    $self->_log( 'mise à jour des liens, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setUpdateLinks();
                    last SWITCH;
                }

                $self->_log( 'type de mise à jour inconnu \''.$self->{'updateType'}.'\'', 3 );
                return undef;
            }

            $self->{'entitiesDescList'} = undef;
            return $self->{'currentEntity'};
        }
    }

    $self->{'currentEntity'} = undef;

    return undef;
}


sub _loadEntities {
    my $self = shift;

    $self->_log( 'chargement de la configuration des serveurs de courriers du domaine '.$self->{'parentDomain'}->getDescription().'\'', 3 );

    require OBM::Tools::obmDbHandler;
    my $dbHandler = OBM::Tools::obmDbHandler->instance();

    if( !$dbHandler ) {
        $self->_log( 'connexion à la base de données impossible', 1 );
        return 1;
    }

    my $hostTable = 'Host';
    my $domainEntity = 'DomainEntity';
    my $serviceProperty = 'ServiceProperty';
    if( $self->{'updateType'} !~ /^(UPDATE_ALL|UPDATE_ENTITY)$/ ) {
        $hostTable = 'P_'.$hostTable;
        $domainEntity = 'P_'.$domainEntity;
        $serviceProperty = 'P_'.$serviceProperty;
    }

    my $query = 'SELECT host_id as server_id,
                        host_name as server_name,
                        serviceproperty_property as server_role
                 FROM '.$hostTable.'
                 INNER JOIN DomainEntity ON domainentity_domain_id='.$self->{'domainId'}.'
                 INNER JOIN ServiceProperty ON serviceproperty_entity_id=domainentity_entity_id
                 WHERE host_id='.$dbHandler->castAsInteger('serviceproperty_value').' AND serviceproperty_service=\'mail\'';

    if( !defined($dbHandler->execQuery( $query, \$self->{'entitiesDescList'} )) ) {
        $self->_log( 'chargement de la configuration des serveurs de courriers depuis la BD impossible', 1 );
        return 1;
    }

    return 0;
}


sub _loadMailServerLinks {
    my $self = shift;

    return 0;
}
