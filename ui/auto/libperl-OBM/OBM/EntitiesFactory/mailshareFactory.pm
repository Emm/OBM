package OBM::EntitiesFactory::mailshareFactory;

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
    my( $updateType, $parentDomain, $ids ) = @_;

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

    if( defined($ids) && (ref($ids) ne 'ARRAY') ) {
        $self->_log( 'liste d\'ID à traiter incorrecte', 1 );
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

    $self->_log( 'obtention de l\'entité suivante', 4 );

    if( !$self->isRunning() ) {
        if( !$self->_start() ) {
            $self->_reset();
            return undef;
        }
    }

    while( defined($self->{'entitiesDescList'}) && (my $mailshareDesc = $self->{'entitiesDescList'}->fetchrow_hashref()) ) {
        require OBM::Entities::obmMailshare;
        if( !(my $current = OBM::Entities::obmMailshare->new( $self->{'parentDomain'}, $mailshareDesc )) ) {
            next;
        }else {
            $self->{'currentEntity'} = $current;

            SWITCH: {
                if( $self->{'updateType'} eq 'UPDATE_ALL' ) {
                    if( $self->_loadMailshareLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 1 );
                        next;
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
                    if( $self->_loadMailshareLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 1 );
                        next;
                    }

                    $self->_log( 'mise à jour des liens, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setUpdateLinks();
                    last SWITCH;
                }

                if( $self->{'updateType'} eq 'SYSTEM_ALL' ) {
                    if( $self->_loadMailshareLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 1 );
                        next;
                    }

                    $self->_log( 'mise à jour de l\'entité et des liens, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setUpdateEntity();
                    $self->{'currentEntity'}->setUpdateLinks();
                    $self->{'currentEntity'}->unsetBdUpdate();
                    last SWITCH;
                }

                if( $self->{'updateType'} eq 'SYSTEM_ENTITY' ) {
                    $self->_log( 'mise à jour de l\'entité, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setUpdateEntity();
                    $self->{'currentEntity'}->unsetBdUpdate();
                    last SWITCH;
                }

                if( $self->{'updateType'} eq 'SYSTEM_LINKS' ) {
                    if( $self->_loadMailshareLinks() ) {
                        $self->_log( 'probleme au chargement des liens de l\'entité '.$self->{'currentEntity'}->getDescription(), 1 );
                        next;
                    }

                    $self->_log( 'mise à jour des liens, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setUpdateLinks();
                    $self->{'currentEntity'}->unsetBdUpdate();
                    last SWITCH;
                }

                if( $self->{'updateType'} eq 'DELETE' ) {
                    $self->_log( 'suppression de l\'entité, '.$self->{'currentEntity'}->getDescription(), 3 );
                    $self->{'currentEntity'}->setDelete();
                    last SWITCH;
                }

                $self->_log( 'type de mise à jour inconnu \''.$self->{'updateType'}.'\'', 1 );
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

    $self->_log( 'chargement des mailshare du domaine d\'identifiant \''.$self->{'domainId'}.'\'', 3 );

    require OBM::Tools::obmDbHandler;
    my $dbHandler = OBM::Tools::obmDbHandler->instance();

    if( !$dbHandler ) {
        $self->_log( 'connexion à la base de données impossible', 1 );
        return 1;
    }

    my $mailshareTablePrefix = '';
    if( $self->{'updateType'} !~ /^(UPDATE_ALL|UPDATE_ENTITY)$/ ) {
        $mailshareTablePrefix = 'P_';
    }

    my $query = 'SELECT '.$mailshareTablePrefix.'MailShare.*,
                        current.mailshare_name as mailshare_name_current
                 FROM '.$mailshareTablePrefix.'MailShare
                 LEFT JOIN P_MailShare current ON current.mailshare_id='.$mailshareTablePrefix.'MailShare.mailshare_id
                 WHERE '.$mailshareTablePrefix.'MailShare.mailshare_domain_id='.$self->{'domainId'};

    if( $self->{'ids'} ) {
        $query .= ' AND '.$mailshareTablePrefix.'MailShare.mailshare_id IN ('.join( ', ', @{$self->{'ids'}}).')';
    }

    $query .= ' ORDER BY '.$mailshareTablePrefix.'MailShare.mailshare_name';

    if( !defined($dbHandler->execQuery( $query, \$self->{'entitiesDescList'} )) ) {
        $self->_log( 'chargement des mailshare depuis la BD impossible', 1 );
        return 1;
    }

    return 0;
}


sub _loadMailshareLinks {
    my $self = shift;
    my %rightDef;

    $self->_log( 'chargement des liens de '.$self->{'currentEntity'}->getDescription(), 3 );

    my $entityId = $self->{'currentEntity'}->getId();

    my $userObmTable = 'UserObm';
    my $userEntityTable = 'UserEntity';
    my $mailshareEntity = 'MailshareEntity';
    my $groupEntityTable = 'GroupEntity';
    my $entityRightTable = 'EntityRight';
    my $ofUserGroupTable = 'of_usergroup';
    if( $self->{'updateType'} =~ /^(SYSTEM_ALL|SYSTEM_ENTITY|SYSTEM_LINKS)$/ ) {
        $userObmTable = 'P_'.$userObmTable;
        $userEntityTable = 'P_'.$userEntityTable;
        $mailshareEntity = 'P_'.$mailshareEntity;
        $groupEntityTable = 'P_'.$groupEntityTable;
        $entityRightTable = 'P_'.$entityRightTable;
        $ofUserGroupTable = 'P_'.$ofUserGroupTable;
    }
    
    $rightDef{'read'}->{'compute'} = 1;
    $rightDef{'read'}->{'sqlQuery'} = 'SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$userEntityTable.' ON userentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$userObmTable.' ON userobm_id = userentity_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_write=0 AND entityright_read=1 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                UNION
                SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$groupEntityTable.' ON groupentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$ofUserGroupTable.' ON of_usergroup_group_id = groupentity_group_id
                INNER JOIN '.$userObmTable.' ON userobm_id = of_usergroup_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_write=0 AND entityright_read=1 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                ORDER BY userobm_login';

    $rightDef{'writeonly'}->{'compute'} = 1;
    $rightDef{'writeonly'}->{'sqlQuery'} = 'SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$userEntityTable.' ON userentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$userObmTable.' ON userobm_id = userentity_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_write=1 AND entityright_read=0 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                UNION
                SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$groupEntityTable.' ON groupentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$ofUserGroupTable.' ON of_usergroup_group_id = groupentity_group_id
                INNER JOIN '.$userObmTable.' ON userobm_id = of_usergroup_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_write=1 AND entityright_read=0 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                ORDER BY userobm_login';

    $rightDef{'write'}->{'compute'} = 1;
    $rightDef{'write'}->{'sqlQuery'} = 'SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$userEntityTable.' ON userentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$userObmTable.' ON userobm_id = userentity_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_write=1 AND entityright_read=1 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                UNION
                SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$groupEntityTable.' ON groupentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$ofUserGroupTable.' ON of_usergroup_group_id = groupentity_group_id
                INNER JOIN '.$userObmTable.' ON userobm_id = of_usergroup_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_write=1 AND entityright_read=1 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                ORDER BY userobm_login';

    $rightDef{'admin'}->{'compute'} = 1;
    $rightDef{'admin'}->{'sqlQuery'} = 'SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$userEntityTable.' ON userentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$userObmTable.' ON userobm_id = userentity_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_admin=1 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                UNION
                SELECT
                  userobm_id,
                  userobm_login
                FROM '.$entityRightTable.'
                INNER JOIN '.$groupEntityTable.' ON groupentity_entity_id = entityright_consumer_id
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                INNER JOIN '.$ofUserGroupTable.' ON of_usergroup_group_id = groupentity_group_id
                INNER JOIN '.$userObmTable.' ON userobm_id = of_usergroup_user_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_admin=1 AND userobm_status=\'VALID\' AND userobm_archive=0 AND userobm_mail_perms=1
                ORDER BY userobm_login';

    $rightDef{'public'}->{'compute'} = 0;
    $rightDef{'public'}->{'sqlQuery'} = 'SELECT
                  entityright_read,
                  entityright_write
                FROM '.$entityRightTable.'
                INNER JOIN '.$mailshareEntity.' ON mailshareentity_entity_id = entityright_entity_id
                WHERE mailshareentity_mailshare_id = '.$entityId.' AND entityright_consumer_id IS NULL';


    $self->{'currentEntity'}->setLinks( $self->_getEntityRight( \%rightDef ) );

    return 0;
}
