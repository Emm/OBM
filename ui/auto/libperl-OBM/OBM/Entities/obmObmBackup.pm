package OBM::Entities::obmObmBackup;

$VERSION = '1.0';

use OBM::Entities::entities;
@ISA = ('OBM::Entities::entities');

$debug = 1;

use 5.006_001;
require Exporter;
use strict;

use OBM::Parameters::common;

use constant LDAPDN => 'obmBackup';


# Needed
sub new {
    my $class = shift;
    my( $parent, $obmBackupDesc ) = @_;

    my $self = bless { }, $class;

    if( ref($parent) ne 'OBM::Entities::obmDomain' ) {
        $self->_log( 'domaine père incorrect', 1 );
        return undef;
    }
    $self->setParent( $parent );

    if( $self->_init( $obmBackupDesc ) ) {
        $self->_log( 'problème lors de l\'initialisation de la configuration du service backup', 1 );
        return undef;
    }

    $self->{'objectclass'} = [ 'obmBackup' ];

    return $self;
}


# Needed
sub DESTROY {
    my $self = shift;

    $self->_log( 'suppression de l\'objet', 5 );

    $self->{'parent'} = undef;
}


# Needed
sub _init {
    my $self = shift;
    my( $obmBackupDesc ) = @_;

    if( !defined($obmBackupDesc) || (ref($obmBackupDesc) ne 'HASH') ) {
        $self->_log( 'description du service de backup incorrecte', 1 );
        return 1;
    }

    $self->{'entityDesc'}->{'ftpHost'} = $obmBackupDesc->{'host_name'};

    return 0;
}


sub setLinks {
    my $self = shift;
    my( $links ) = @_;

    return 0;
}


# Needed
sub getDescription {
    my $self = shift;

    my $description = 'configuration OBM du domaine '.$self->{'parent'}->getDesc('domain_name');

    return $description;
}


# Needed
sub getDomainId {
    my $self = shift;

    return 0;
}


# Needed
sub getId {
    my $self = shift;

    return 0;
}


# Needed by : LdapEngine
sub getLdapServerId {
    my $self = shift;

    if( defined($self->{'parent'}) ) {
        return $self->{'parent'}->getLdapServerId();
    }

    return undef;
}


# Needed by : LdapEngine
sub setParent {
    my $self = shift;
    my( $parent ) = @_;

    if( ref($parent) ne 'OBM::Entities::obmDomain' ) {
        $self->_log( 'description du domaine parent incorrecte', 1 );
        return 1;
    }

    $self->{'parent'} = $parent;

    return 0;
}


# Needed by : LdapEngine
sub getDnPrefix {
    my $self = shift;
    my $rootDn;
    my @dnPrefixes;

    if( !($rootDn = $self->_getParentDn()) ) {
        $self->_log( 'DN de la racine du domaine parent non déterminée', 1 );
        return undef;
    }

    for( my $i=0; $i<=$#{$rootDn}; $i++ ) {
        push( @dnPrefixes, 'cn='.LDAPDN.','.$rootDn->[$i] );
        $self->_log( 'DN de l\'entité : '.$dnPrefixes[$i], 4 );
    }

    return \@dnPrefixes;
}


# Needed by : LdapEngine
sub getCurrentDnPrefix {
    my $self = shift;

    return $self->getDnPrefix();
}


sub _getLdapObjectclass {
    my $self = shift;
    my ($objectclass, $deletedObjectclass) = @_;
    my %realObjectClass;

    if( !defined($objectclass) || (ref($objectclass) ne 'ARRAY') ) {
        $objectclass = $self->{'objectclass'};
    }

    for( my $i=0; $i<=$#$objectclass; $i++ ) {
        $realObjectClass{$objectclass->[$i]} = 1;
    }

    my @realObjectClass = keys(%realObjectClass);

    return \@realObjectClass;
}


sub createLdapEntry {
    my $self = shift;
    my ( $entryDn, $entry ) = @_;

    if( !$entryDn ) {
        $self->_log( 'DN non défini', 1 );
        return 1;
    }

    if( ref($entry) ne 'Net::LDAP::Entry' ) {
        $self->_log( 'entrée LDAP incorrecte', 1 );
        return 1;
    }

    $entry->add(
        objectClass => $self->_getLdapObjectclass(),
        cn => LDAPDN
    );

    # OBM domain language
    if( $self->{'entityDesc'}->{'ftpHost'} ) {
        $entry->add( ftpHost => $self->{'entityDesc'}->{'ftpHost'} );
    }

    # OBM domain
    if( defined($self->{'parent'}) && (my $domainName = $self->{'parent'}->getDesc('domain_name')) ) {
        $entry->add( obmDomain => $domainName );
    }

    return 0;
}


sub updateLdapEntry {
    my $self = shift;
    my( $entry, $objectclassDesc ) = @_;
    my $update = 0;

    if( ref($entry) ne 'Net::LDAP::Entry' ) {
        return $update;
    }


    if( $self->getUpdateEntity() ) {
        # Vérification des objectclass
        my @deletedObjectclass;
        my $currentObjectclass = $self->_getLdapObjectclass( $entry->get_value('objectClass', asref => 1), \@deletedObjectclass );
        if( $self->_modifyAttrList( $currentObjectclass, $entry, 'objectClass' )) {
            $update = 1;
        }

        if( $#deletedObjectclass >= 0 ) {
            # Pour les schémas LDAP supprimés, on détermine les attributs à
            # supprimer.
            # Uniquement ceux qui ne sont pas utilisés par d'autres objets.
            my $deleteAttrs = $self->_diffObjectclassAttrs(\@deletedObjectclass, $currentObjectclass, $objectclassDesc);

            for( my $i=0; $i<=$#$deleteAttrs; $i++ ) {
                if( $self->_modifyAttrList( undef, $entry, $deleteAttrs->[$i] ) ) {
                    $update = 1;
                }
            }
        }

        # OBM domain language
        if( $self->_modifyAttr( $self->{'entityDesc'}->{'ftpHost'}, $entry, 'ftpHost' ) ) {
            $update = 1;
        }

        # OBM domain
        if( defined($self->{'parent'}) && (my $domainName = $self->{'parent'}->getDesc('domain_name')) ) {
            if( $self->_modifyAttr( $domainName, $entry, 'obmDomain' ) ) {
                $update = 1;
            }
        }
    }

    return $update;
}
