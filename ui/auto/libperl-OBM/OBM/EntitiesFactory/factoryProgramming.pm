package OBM::EntitiesFactory::factoryProgramming;

$VERSION = '1.0';

use OBM::EntitiesFactory::factory;
use OBM::Log::log;
@ISA = ('OBM::EntitiesFactory::factory', 'OBM::Log::log');

$debug = 1;

use 5.006_001;
require Exporter;
use strict;


sub new {
    my $class = shift;

    my $self = bless { }, $class;

    return $self;
}


sub DESTROY {
    my $self = shift;

    $self->_log( 'suppression de l\'objet', 5 );
}


sub _initFactoryProgramming {
    my $self = shift;

    $self->_log( 'initialisation du programmateur de factory', 4 );
    $self->{'entityType'} = undef;
    $self->{'updateType'} = undef;
    $self->{'entitiesId'} = undef;

    return 0;
}


sub setEntitiesType {
    my $self = shift;
    my( $type ) = @_;

    $self->_initFactoryProgramming();

    SWITCH: {
        if( $type eq 'USER' ) {
            $self->_log( 'initialisation d\'un programmateur d\'entité de type utilisateur', 4 );
            $self->{'entityType'} = $type;
            last SWITCH;
        }

        if( $type eq 'MAILSHARE' ) {
            $self->_log( 'initialisation d\'un programmateur d\'entité de type partage messagerie', 4 );
            $self->{'entityType'} = $type;
            last SWITCH;
        }

        if( $type eq 'CONTACT' ) {
            $self->_log( 'initialisation d\'un programmateur d\'entité de type contacts', 4 );
            $self->{'entityType'} = $type;
            last SWITCH;
        }

        if( $type eq 'GROUP' ) {
            $self->_log( 'initialisation d\'un programmateur d\'entité de type groupe', 4 );
            $self->{'entityType'} = $type;
            last SWITCH;
        }

        if( $type eq 'HOST' ) {
            $self->_log( 'initialisation d\'un programmateur d\'entité de type hôte', 4 );
            $self->{'entityType'} = $type;
            last SWITCH;
        }

        if( $type eq 'CONTACT_SERVICE' ) {
            $self->_log( 'initialisation d\'un programmateur d\'entité de type configuration du service contacts', 3 );
            $self->{'entityType'} = $type;
            last SWITCH;
        }

        $self->_log( 'type d\'entité inconnu \''.$type.'\'', 3 );
        return 1;
    }

    return 0;
}


sub getEntitiesType {
    my $self = shift;

    return $self->{'entityType'};
}


sub setUpdateType {
    my $self = shift;
    my( $updateType ) = @_;

    $self->{'updateType'} = $updateType;

    if( !$self->_checkUpdateType() ) {
        $self->{'updateType'} = undef;
        return 1;
    }

    return 0;
}


sub getUpdateType {
    my $self = shift;

    return $self->{'updateType'};
}


sub setEntitiesIds {
    my $self = shift;
    my( $entitiesId ) = @_;

    if( ref($entitiesId) ne 'ARRAY' ) {
        $self->_log( 'listes d\'identifiant incorrecte', 1 );
        return 1;
    }

    $self->{'entitiesId'} = $entitiesId;

    return 0;
}


sub getEntitiesIds {
    my $self = shift;

    return $self->{'entitiesId'};
}
