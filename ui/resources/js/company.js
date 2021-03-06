Obm.Company = {}

Obm.Company.PhoneWidget = new Class ({
  kind : 'phones',
  structure : {
    number: { kind: 'text', value: '', newLine : true, label : obm.vars.labels.phoneNumber},
    label: { kind: 'select', value: 'WORK',token: obm.vars.labels.phoneLabel, newCell : true, label : ''}      
  },

  options: {container: 'phoneHolder'},
  
  newId: function() {if(!Obm.Company.PhoneWidget.phoneId) Obm.Company.PhoneWidget.phoneId = 0; return Obm.Company.PhoneWidget.phoneId++;},

  Extends: Obm.CoordonateWidget
});

Obm.Company.EmailWidget = new Class ({
  kind : 'emails',
  structure : {
    address: { kind: 'text', value: '', newLine : true, label : obm.vars.labels.emailAddress},
    label: { kind: 'select', value: 'WORK',token: obm.vars.labels.emailLabel, newCell : true, label : ''}      
  },

  options: {container: 'emailHolder'},

  newId: function() {if(!Obm.Company.EmailWidget.emailId) Obm.Company.EmailWidget.emailId = 0; return Obm.Company.EmailWidget.emailId++;},

  Extends: Obm.CoordonateWidget
});

Obm.Company.AddressWidget = new Class ({
  kind : 'addresses',
  structure : {
    street: { kind: 'textarea', value: '', rows: 3, newLine : true, label : obm.vars.labels.addressStreet},
    label: { kind: 'select', value: 'WORK',token: obm.vars.labels.addressLabel, label : ''}, 
    zipcode: { kind: 'text', value: '' , newCell : true, label : obm.vars.labels.addressZipcode},
    town: { kind: 'text', value: '', label : obm.vars.labels.addressTown },
    expresspostal: { kind: 'text', value: '', label : obm.vars.labels.addressExpressPostal },
    country: { kind: 'select', value: '', token: obm.vars.labels.countries, label : obm.vars.labels.addressCountry }
  },

  options: {container: 'addressHolder'},

  newId: function() {if(!Obm.Company.AddressWidget.addressId) Obm.Company.AddressWidget.addressId = 0; return Obm.Company.AddressWidget.addressId++;},

  Extends: Obm.CoordonateWidget
});

Obm.Company.WebsiteWidget = new Class ({
  kind : 'websites',
  structure : {
    url: { kind: 'text', value: '', newLine : true, label: obm.vars.labels.websiteUrl},
    label: { kind: 'select', value: 'HOMEPAGE',token: obm.vars.labels.websiteLabel, newCell : true, label: ''}      
  },

  options: {container: 'websiteHolder'},

  newId: function() {if(!Obm.Company.WebsiteWidget.websiteId) Obm.Company.WebsiteWidget.websiteId = 0; return Obm.Company.WebsiteWidget.websiteId++;},

  Extends: Obm.CoordonateWidget
});

Obm.Company.IMWidget = new Class ({
  kind : 'ims',
  structure : {
    address: { kind: 'text', value: '', newLine : true, label: obm.vars.labels.imAddress},
    protocol: { kind: 'select', value: 'JABBER', token : {'XMPP' : 'Jabber', 'X-GTALK' : 'Google Talk', 'AIM' : 'AIM', 'YMSGR' : 'Yahoo', 'MSN' : 'MSN', 'X-ICQ' : 'ICQ'}, newCell : true, label: ''}      
  },

  options: {container: 'imHolder'},
  
  newId: function() {if(!Obm.Company.IMWidget.imId) Obm.Company.IMWidget.imId = 0; return Obm.Company.IMWidget.imId++;},

  Extends: Obm.CoordonateWidget
});
/*  Implements: Options,

  options: {
    mode: 'update'
  },

  initialize: function(label, value, options) {
    this.setOptions(options)
    document.fireEvent('widgetadded');
    this.label = label;
    this.value = value;
    this.element = new Element('table');
    if(this.options.mode == 'consult') {

    } else {
      this.updateMode(); 
    }
    $('phoneHolder').adopt(this.element);
    document.addEvent('widgetadded', this.consultMode.bind(this));
  },

  updateMode: function() {
    this.element.set('html','');
    this.element.adopt(
      new Element('tr').adopt(
        new Element('th').appendText('Label')
      ).adopt(
        new Element('td').adopt(
          new Element('input').setProperties({
            'type' : 'text',
            'value' : this.label,
            'name' : 'phone_label[]'
          })
        )
      )
    ).adopt(
      new Element('tr').adopt(
        new Element('th').appendText('Numéros')
      ).adopt(
        new Element('td').adopt(
          new Element('input').setProperties({
            'type' : 'text',
            'value' : this.value,
            'name' : 'phone_number[]'
          }).addEvent('enter', this.consultMode.bind(this))
        )
      ).adopt(
        new Element('td').setProperty('colspan', 2).adopt(          
          new Element('input').setProperties({
            'type' : 'button',
            'value' : 'Valider'
          }).addEvent('click', this.consultMode.bind(this))
        )
      )
    )
  },

  consultMode: function() {
    this.element.set('html','');
    this.element.adopt(
      new Element('tr').adopt(
        new Element('th').appendText(this.label).addEvent('click', this.updateMode.bind(this))
      ).adopt(
        new Element('td').appendText(this.value)
          .addEvent('mouseover', function() {
            this.addClass('editable')
          }).addEvent('mouseout', function () {
            this.removeClass('editable')
          })        
          .addEvent('click', this.updateMode.bind(this))
      )


    );
  }
});*/
