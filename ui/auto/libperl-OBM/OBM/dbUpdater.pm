package OBM::dbUpdater;

$VERSION = '1.0';

use OBM::Log::log;
@ISA = ('OBM::Log::log');

$debug = 1;

use 5.006_001;
require Exporter;
use strict;


sub new {
    my $class = shift;

    my $self = bless { }, $class;

    $self->{'counter'} = 0;

    return $self;
}


sub DESTROY {
    my $self = shift;

    $self->_log( 'suppression de l\'objet', 4 );
}


sub reset {
    my $self = shift;

    $self->{'counter'} = 0;
}


sub update {
    my $self = shift;
    my( $entity ) = @_;

    if( !defined($entity) || !ref($entity) ) {
        $self->_log( 'entité à mettre à jour incorrecte', 3 );
        return 1;
    }

    $self->{'entity'} = $entity;

    my $returnCode = 0;
    SWITCH: {
        if( !$entity->getDelete() && !$entity->getBdUpdate() ) {
            $self->_log( 'l\'entité '.$entity->getDescription().' n\'est pas à mettre à jour en BD', 3 );
            last SWITCH;
        }

        if( !$entity->getUpdated() ) {
            $self->_log( 'l\'entité '.$entity->getDescription().' est en erreur de traitement', 3 );
            $self->_log( 'pas de traitement BD de l\'entité de '.$entity->getDescription(), 2 );
            $returnCode = 1;
            last SWITCH;
        }

        if( $entity->getDelete() ) {
            $returnCode = $self->_delete();
            last SWITCH;
        }

        if( $entity->getUpdated() ) {
            $returnCode = $self->_update();
            last SWITCH;
        }
    }
    
    $self->_updateCounter();

    return $returnCode;
}


sub _updateCounter {
    my $self = shift;

    require OBM::Tools::obmDbHandler;
    my $dbHandler;
    if( !($dbHandler = OBM::Tools::obmDbHandler->instance()) ) {
        $self->_log( 'connexion à la base de données impossible', 3 );
        return 1;
    }

    $self->{'counter'}++;
    my $query = 'UPDATE ObmInfo SET obminfo_value='.$self->{'counter'}.' WHERE obminfo_name=\'scope-progress\'';

    my $sth;
    $dbHandler->execQuery( $query, \$sth );
    
    return 0;
}


sub _delete {
    my $self = shift;

    my $returnCode = 0;
    SWITCH: {
        if( ref($self->{'entity'}) eq 'OBM::Entities::obmDomain' ) {
            require OBM::DbUpdater::domainUpdater;
            my $domainUpdater;
            if( !($domainUpdater = OBM::DbUpdater::domainUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'suppression BD '.$self->{'entity'}->getDescription(), 3 );
            $returnCode = $domainUpdater->delete( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmMailServer' ) {
            require OBM::DbUpdater::mailServerUpdater;
            my $mailServerUpdater;
            if( !($mailServerUpdater = OBM::DbUpdater::mailServerUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'suppression BD '.$self->{'entity'}->getDescription(), 3 );
            $returnCode = $mailServerUpdater->delete( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmHost' ) {
            require OBM::DbUpdater::hostUpdater;
            my $hostUpdater;
            if( !($hostUpdater = OBM::DbUpdater::hostUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'suppression BD '.$self->{'entity'}->getDescription(), 3 );
            $returnCode = $hostUpdater->delete( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmGroup' ) {
            require OBM::DbUpdater::groupUpdater;
            my $groupUpdater;
            if( !($groupUpdater = OBM::DbUpdater::groupUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'suppression BD '.$self->{'entity'}->getDescription(), 3 );
            $returnCode = $groupUpdater->delete( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmMailshare' ) {
            require OBM::DbUpdater::mailshareUpdater;
            my $mailshareUpdater;
            if( !($mailshareUpdater = OBM::DbUpdater::mailshareUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'suppression BD '.$self->{'entity'}->getDescription(), 3 );
            $returnCode = $mailshareUpdater->delete( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmUser' ) {
            require OBM::DbUpdater::userUpdater;
            my $userUpdater;
            if( !($userUpdater = OBM::DbUpdater::userUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'suppression BD '.$self->{'entity'}->getDescription(), 3 );
            $returnCode = $userUpdater->delete( $self->{'entity'} );

            last SWITCH;
        }

        $self->_log( 'entité de type inconnu, pas de traitements effectués', 1 );
    }

    return $returnCode;
}


sub _update {
    my $self = shift;

    my $returnCode = 0;
    SWITCH: {
        if( ref($self->{'entity'}) eq 'OBM::Entities::obmDomain' ) {
            require OBM::DbUpdater::domainUpdater;
            my $domainUpdater;
            if( !($domainUpdater = OBM::DbUpdater::domainUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'mise à jour BD '.$self->{'entity'}->getDescription(), 3);
            $returnCode = $domainUpdater->update( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmMailServer' ) {
            require OBM::DbUpdater::mailServerUpdater;
            my $mailServerUpdater;
            if( !($mailServerUpdater = OBM::DbUpdater::mailServerUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'mise à jour BD '.$self->{'entity'}->getDescription(), 3);
            $returnCode = $mailServerUpdater->update( $self->{'entity'} );

            last SWITCH;
        }

         if( ref($self->{'entity'}) eq 'OBM::Entities::obmHost' ) {
            require OBM::DbUpdater::hostUpdater;
            my $hostUpdater;
            if( !($hostUpdater = OBM::DbUpdater::hostUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'mise à jour BD '.$self->{'entity'}->getDescription(), 3 );
            $returnCode = $hostUpdater->update( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmGroup' ) {
            require OBM::DbUpdater::groupUpdater;
            my $groupUpdater;
            if( !($groupUpdater = OBM::DbUpdater::groupUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'mise à jour BD '.$self->{'entity'}->getDescription(), 3 );

            $returnCode = $groupUpdater->update( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmMailshare' ) {
            require OBM::DbUpdater::mailshareUpdater;
            my $mailshareUpdater;
            if( !($mailshareUpdater = OBM::DbUpdater::mailshareUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'mise à jour BD '.$self->{'entity'}->getDescription(), 3 );

            $returnCode = $mailshareUpdater->update( $self->{'entity'} );

            last SWITCH;
        }

        if( ref($self->{'entity'}) eq 'OBM::Entities::obmUser' ) {
            require OBM::DbUpdater::userUpdater;
            my $userUpdater;
            if( !($userUpdater = OBM::DbUpdater::userUpdater->new()) ) {
                $returnCode = 1;
                last SWITCH;
            }

            $self->_log( 'mise à jour BD '.$self->{'entity'}->getDescription(), 3 );

            $returnCode = $userUpdater->update( $self->{'entity'} );

            last SWITCH;
        }

        $self->_log( 'entité de type inconnu, pas de traitements effectués', 1 );
    }

    return $returnCode;
}
