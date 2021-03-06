(function() {
  var Notepad, SheetPlugin, TemplateConfig, TemplateManager, Whiskey2,
    __slice = Array.prototype.slice;

  yepnope({
    load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/whiskey2.css', 'lib/lima1/net.js', 'lib/lima1/main.js', 'bs/js/bootstrap-datepicker.js', 'bs/css/datepicker.css', 'lib/common-web/canto-0.15.js', 'js/w2plugins.js'],
    complete: function() {
      return $(document).ready(function() {
        var app;
        app = new Whiskey2;
        return app.start();
      });
    }
  });

  Whiskey2 = (function() {

    function Whiskey2() {}

    Whiskey2.prototype.start = function() {
      var cls, db, jqnet, key, storage,
        _this = this;
      db = new HTML5Provider('app.db', '1');
      storage = new StorageProvider(db);
      jqnet = new jQueryTransport('https://lima1-kvj.rhcloud.com');
      this.oauth = new OAuthProvider({
        clientID: 'whiskey2web'
      }, jqnet);
      this.manager = new Lima1DataManager('whiskey2', this.oauth, storage);
      this.syncProgress = $(document.createElement('div')).addClass('progress progress-striped active');
      this.syncProgressBar = $(document.createElement('div')).addClass('bar').appendTo(this.syncProgress);
      this.manager.on_sync.on('start', function() {
        return _this.syncAlert = _this.showAlert('Please stand by...', {
          persistent: true,
          content: _this.syncProgress
        });
      });
      this.manager.on_sync.on('finish', function() {
        return _this.syncAlert.remove();
      });
      this.manager.on_scheduled_sync = function() {
        return _this.sync();
      };
      this.oauth.token = this.manager.get('token', 'no-value');
      this.templateConfigs = {};
      this.sheetPlugins = {};
      this.templateDrawer = new DrawTemplate(this);
      for (key in templateConfigs) {
        cls = templateConfigs[key];
        this.templateConfigs[key] = new cls(this);
      }
      for (key in sheetPlugins) {
        cls = sheetPlugins[key];
        this.sheetPlugins[key] = new cls(this, key);
      }
      return this.manager.open(function(error) {
        if (error) {
          _this.manager = null;
          _this.showError(error);
        }
        _this.manager.storage.cache = new HTML5CacheProvider(_this.oauth, _this.manager.app, 1280);
        _this.showAlert('Application started', {
          severity: 'success'
        });
        _this.oauth.on_token_error = function() {
          return _this.showLoginDialog();
        };
        _this.bindMain();
        _this.refreshBookmarks(function() {
          _this.refreshNotepads();
          return _this.refreshTemplates();
        });
        _this.manager.start_ping(function(err, haveData) {
          if (haveData) {
            return _this.sync(function() {
              if (_this.selected) return _this.selected.reloadSheets();
            });
          }
        });
        return _this.sync(function() {
          var id, notepadID;
          notepadID = null;
          if (_this.selected) return;
          for (id in _this.notepads) {
            notepadID = id;
            break;
          }
          if (notepadID) return _this.selectNotepad(notepadID);
        });
      });
    };

    Whiskey2.prototype.showLoginDialog = function() {
      $('#main-password').val('');
      return $('#main-login-dialog').modal('show');
    };

    Whiskey2.prototype.bindMain = function() {
      var templatesManager,
        _this = this;
      templatesManager = new TemplateManager(this);
      this.templatesManager = templatesManager;
      $('#main-sync').bind('click', function() {
        return _this.sync();
      });
      $('#main-do-login').bind('click', function() {
        return _this.doLogin();
      });
      $('#main-do-login-close').bind('click', function() {
        return $('#main-login-dialog').modal('hide');
      });
      $('#main-add-notepad').bind('click', function() {
        $('#main-add-notepad-dialog').modal('show');
        return $('#main-add-notepad-name').val('');
      });
      $('#main-do-add-notepad').bind('click', function() {
        return _this.doAddNotepad();
      });
      return $('#main-tab-templates a').bind('click', function() {
        $(this).tab('show');
        return templatesManager.refresh();
      });
    };

    Whiskey2.prototype.dragSetType = function(e, type, value) {
      var _ref, _ref2;
      if (e != null ? (_ref = e.originalEvent) != null ? (_ref2 = _ref.dataTransfer) != null ? _ref2.setData : void 0 : void 0 : void 0) {
        return e.originalEvent.dataTransfer.setData(type, JSON.stringify(value));
      }
    };

    Whiskey2.prototype.dragGetType = function(e, type) {
      var _ref, _ref2, _ref3, _ref4, _ref5, _ref6;
      if ('Files' === type && (e != null ? (_ref = e.originalEvent) != null ? (_ref2 = _ref.dataTransfer) != null ? _ref2.files : void 0 : void 0 : void 0)) {
        return e.originalEvent.dataTransfer.files;
      }
      if (e != null ? (_ref3 = e.originalEvent) != null ? (_ref4 = _ref3.dataTransfer) != null ? _ref4.getData : void 0 : void 0 : void 0) {
        try {
          return JSON.parse(e != null ? (_ref5 = e.originalEvent) != null ? (_ref6 = _ref5.dataTransfer) != null ? _ref6.getData(type) : void 0 : void 0 : void 0);
        } catch (e) {
          return null;
        }
      }
      return null;
    };

    Whiskey2.prototype.dragHasType = function() {
      var e, t, type, types, _i, _j, _len, _len2, _ref, _ref2, _ref3, _ref4, _ref5;
      e = arguments[0], types = 2 <= arguments.length ? __slice.call(arguments, 1) : [];
      if (!(e != null ? (_ref = e.originalEvent) != null ? (_ref2 = _ref.dataTransfer) != null ? _ref2.types : void 0 : void 0 : void 0)) {
        return false;
      }
      _ref5 = e != null ? (_ref3 = e.originalEvent) != null ? (_ref4 = _ref3.dataTransfer) != null ? _ref4.types : void 0 : void 0 : void 0;
      for (_i = 0, _len = _ref5.length; _i < _len; _i++) {
        type = _ref5[_i];
        for (_j = 0, _len2 = types.length; _j < _len2; _j++) {
          t = types[_j];
          if (type === t) return true;
        }
      }
      return false;
    };

    Whiskey2.prototype.dragGetOffset = function(e, div) {
      var offset;
      offset = {
        left: e.originalEvent.clientX,
        top: e.originalEvent.clientY
      };
      if (div) {
        offset.left -= div.offset().left;
        offset.top -= div.offset().top;
      }
      return offset;
    };

    Whiskey2.prototype.refreshTemplates = function(handler) {
      var _this = this;
      return this.manager.storage.select('templates', [], function(err, data) {
        var item, _i, _len;
        if (err) {
          if (handler) handler(err);
          return;
        }
        _this.templates = {};
        for (_i = 0, _len = data.length; _i < _len; _i++) {
          item = data[_i];
          _this.templates[item.id] = item;
        }
        if (handler) return handler();
      });
    };

    Whiskey2.prototype.refreshBookmarks = function(handler) {
      var _this = this;
      return this.manager.storage.select('bookmarks', [], function(err, data) {
        var item, _i, _len;
        if (err) return handler(err);
        _this.bookmarks = {};
        for (_i = 0, _len = data.length; _i < _len; _i++) {
          item = data[_i];
          if (!_this.bookmarks[item.sheet_id]) _this.bookmarks[item.sheet_id] = [];
          _this.bookmarks[item.sheet_id].push(item);
        }
        return handler();
      });
    };

    Whiskey2.prototype.refreshNotepads = function(selectID) {
      var _this = this;
      return this.manager.storage.select('notepads', [
        'archived', {
          op: '<>',
          "var": 1
        }
      ], function(err, arr) {
        var a, div, i, item, li, moveNotepad, n, _fn, _ref, _results;
        if (err) return _this.showError(err);
        $('#main-tabs li.main-tab-notepad').remove();
        $('#main-tabs-content .main-tab-notepad').remove();
        _this.notepads = {};
        _this.selected = null;
        _this.sortWithNext(arr);
        moveNotepad = function(index, id) {
          return _this.moveWithNext('notepads', arr, index, [id], function(err) {
            if (err) return _this.showError(err);
            return _this.refreshNotepads(selectID);
          });
        };
        _fn = function(a, item, i) {
          a.bind('dragstart', function(e) {
            return _this.dragSetType(e, 'custom/notepad', {
              id: item.id
            });
          });
          a.bind('dragover', function(e) {
            if ((_this.dragHasType(e, 'custom/note')) || (_this.dragHasType(e, 'custom/sheet')) || (_this.dragHasType(e, 'custom/notepad'))) {
              return e.preventDefault();
            }
          });
          a.bind('dragenter', function(e) {
            if (_this.dragHasType(e, 'custom/notepad')) e.preventDefault();
            if ((_this.dragHasType(e, 'custom/note')) || (_this.dragHasType(e, 'custom/sheet')) || (_this.dragHasType(e, 'custom/bmark'))) {
              a.tab('show');
              return e.preventDefault();
            }
          });
          a.bind('drop', function(e) {
            var notepadData;
            notepadData = _this.dragGetType(e, 'custom/notepad');
            if (notepadData) {
              moveNotepad(i, notepadData.id);
              return e.stopPropagation();
            }
          });
          return a.bind('click', function(e) {
            e.preventDefault();
            return _this.selectNotepad(item.id);
          });
        };
        _results = [];
        for (i = 0, _ref = arr.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
          item = arr[i];
          li = $(document.createElement('li')).addClass('main-tab-notepad');
          a = $(document.createElement('a')).attr({
            href: '#np' + item.id
          }).text(' ' + item.name).appendTo(li);
          a.prepend('<i class="icon-book"></i>');
          li.insertBefore($('#main-tab-templates'));
          div = $(document.createElement('div')).addClass('tab-pane main-tab-notepad').attr({
            id: 'np' + item.id
          });
          $('#main-tabs-content').append(div);
          _fn(a, item, i);
          n = _this.renderNotepad(div, item);
          n.anchor = a;
          if (item.id === selectID) {
            _results.push(_this.selectNotepad(item.id));
          } else {
            _results.push(void 0);
          }
        }
        return _results;
      });
    };

    Whiskey2.prototype.selectNotepad = function(id) {
      var n;
      n = this.notepads[id];
      if (!n) return this.showError('Notepad not found');
      n.anchor.tab('show');
      n.reloadSheets();
      return this.selected = n;
    };

    Whiskey2.prototype.doAddNotepad = function() {
      var name,
        _this = this;
      name = $('#main-add-notepad-name').val().trim();
      if (!name) return this.showError('Name is empty');
      $('#main-add-notepad-dialog').modal('hide');
      return this.manager.storage.create('notepads', {
        name: name,
        archived: 0
      }, function(err, data) {
        if (err) return _this.showError(err);
        $('#main-add-notepad-dialog').modal('hide');
        _this.showAlert('Notepad created', {
          severity: 'success'
        });
        return _this.refreshNotepads(data.id);
      });
    };

    Whiskey2.prototype.renderNotepad = function(parent, notepad) {
      var div, n;
      div = $('#main-notepad-template').clone().appendTo(parent).removeClass('hide');
      n = new Notepad(this, notepad, div);
      this.notepads[notepad.id] = n;
      return n;
    };

    Whiskey2.prototype.doLogin = function() {
      var login, password,
        _this = this;
      login = $('#main-login').val().trim();
      password = $('#main-password').val().trim();
      log('Checking', login, password);
      $('#main-login-dialog').modal('hide');
      return this.oauth.tokenByUsernamePassword(login, password, function(err) {
        if (err) {
          _this.showLoginDialog();
          return _this.showError(err.error_description);
        }
        _this.showAlert('Login OK', {
          severity: 'success'
        });
        return _this.sync();
      });
    };

    Whiskey2.prototype.sync = function(handler) {
      var _this = this;
      if (!this.manager) return;
      return this.manager.sync(function(err) {
        if (err) return _this.showError(err);
        return _this.refreshTemplates(function() {
          if (handler) return handler();
        });
      }, function(type) {
        var w;
        w = 100;
        switch (type) {
          case 0:
            w = 25;
            break;
          case 1:
            w = 50;
            break;
          case 2:
            w = 75;
        }
        return _this.syncProgressBar.width("" + w + "%");
      });
    };

    Whiskey2.prototype.showError = function(message) {
      return this.showAlert(message, {
        severity: 'error'
      }, 'Error');
    };

    Whiskey2.prototype.showPrompt = function(message, handler) {
      var alert, button, div,
        _this = this;
      div = $(document.createElement('p'));
      button = $(document.createElement('button')).addClass('btn btn-danger').text('Proceed').appendTo(div);
      button.bind('click', function(e) {
        if (handler) handler();
        return alert.remove();
      });
      return alert = this.showAlert(message, {
        persistent: true,
        severity: 'block',
        content: div
      }, 'Prompt');
    };

    Whiskey2.prototype.showAlert = function(message, config, title) {
      var div, _ref, _ref2,
        _this = this;
      if (title == null) title = 'Whiskey2';
      div = $(document.createElement('div')).appendTo($('#alerts')).addClass('alert alert-' + ((_ref = config != null ? config.severity : void 0) != null ? _ref : 'info'));
      $(document.createElement('button')).appendTo(div).addClass('close').attr({
        'data-dismiss': 'alert'
      }).html('&times;');
      $(document.createElement('h4')).appendTo(div).text(title != null ? title : 'Untitled');
      $(document.createElement('span')).appendTo(div).text(message);
      if (config != null ? config.content : void 0) div.append(config.content);
      if (!(config != null ? config.persistent : void 0)) {
        setTimeout(function() {
          return div.remove();
        }, (_ref2 = config != null ? config.timeout : void 0) != null ? _ref2 : 3000);
      }
      return div;
    };

    Whiskey2.prototype.emptyTemplate = {
      width: 102,
      height: 144,
      name: 'No template'
    };

    Whiskey2.prototype.getTemplate = function(id) {
      var tmpl;
      tmpl = this.templates[id];
      if (tmpl) return tmpl;
      return this.emptyTemplate;
    };

    Whiskey2.prototype.getTemplateConfig = function(tmpl) {
      var _ref;
      return (_ref = this.templateConfigs[tmpl != null ? tmpl.type : void 0]) != null ? _ref : null;
    };

    Whiskey2.prototype.findByID = function(arr, id, attr) {
      var i, _ref;
      if (attr == null) attr = 'id';
      if (!id) return -1;
      for (i = 0, _ref = arr.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        if (arr[i][attr] === id) return i;
      }
      return -1;
    };

    Whiskey2.prototype.addUnique = function(array, item, attr) {
      var index;
      if (attr == null) attr = 'id';
      index = this.findByID(array, item[attr], attr);
      if (index === -1) return array.push(item);
    };

    Whiskey2.prototype.removeWithNext = function(sink, array, items) {
      var item, left_index, right_index, _i, _len, _results;
      _results = [];
      for (_i = 0, _len = items.length; _i < _len; _i++) {
        item = items[_i];
        left_index = this.findByID(array, item.id, 'next_id');
        if (left_index !== -1) {
          right_index = this.findByID(array, item.next_id);
          if (right_index !== -1) {
            array[left_index].next_id = item.next_id;
          } else {
            array[left_index].next_id = null;
          }
          _results.push(this.addUnique(sink, array[left_index]));
        } else {
          _results.push(void 0);
        }
      }
      return _results;
    };

    Whiskey2.prototype.addWithNext = function(sink, array, after, items) {
      var i, next_id, right_index, _ref;
      if (!(items != null ? items.length : void 0)) return;
      next_id = null;
      right_index = 0;
      if (after !== -1) {
        right_index = after + 1;
        next_id = array[after].next_id;
        array[after].next_id = items[0].id;
        this.addUnique(sink, array[after]);
      } else {
        if (array.length > 0) next_id = array[0].id;
      }
      for (i = 0, _ref = items.length - 1; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        items[i].next_id = items[i + 1].id;
      }
      return items[items.length - 1].next_id = next_id;
    };

    Whiskey2.prototype.sortWithNext = function(items) {
      var found, i, index, item;
      if (!(items != null ? items.length : void 0)) return items;
      i = 0;
      while (i < items.length) {
        item = items[i];
        index = this.findByID(items, item.next_id);
        if (index !== -1) {
          found = items[index];
          if (index < i) {
            items.splice(i + 1, 0, found);
            items.splice(index, 1);
            i--;
          } else {
            if (index > i + 1) {
              items.splice(index, 1);
              items.splice(i + 1, 0, found);
            }
          }
        }
        i++;
      }
      return items;
    };

    Whiskey2.prototype.moveWithNext = function(stream, array, index, ids, handler, adapter) {
      var ag, id, _i, _len, _results,
        _this = this;
      ag = new AsyncGrouper(ids.length, function() {
        var arr, config, err, item, items, sink, _i, _j, _len, _len2, _ref;
        err = ag.findError();
        if (err) return handler(err);
        items = [];
        sink = [];
        _ref = ag.results;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          arr = _ref[_i];
          items.push(arr[0]);
          sink.push(arr[0]);
        }
        _this.removeWithNext(sink, array, items);
        _this.addWithNext(sink, array, index, items);
        config = [];
        for (_j = 0, _len2 = sink.length; _j < _len2; _j++) {
          item = sink[_j];
          if (adapter) adapter(item);
          config.push({
            type: 'update',
            stream: stream,
            object: item
          });
        }
        return _this.manager.batch(config, function(err) {
          if (err) return handler(err);
          return handler(null);
        });
      });
      _results = [];
      for (_i = 0, _len = ids.length; _i < _len; _i++) {
        id = ids[_i];
        _results.push(this.manager.findOne(stream, id, ag.fn));
      }
      return _results;
    };

    Whiskey2.prototype.sortTemplates = function(arr) {
      var item, result, section, tag, _i, _len;
      result = [];
      tag = null;
      section = null;
      for (_i = 0, _len = arr.length; _i < _len; _i++) {
        item = arr[_i];
        if (item.tag !== tag) {
          section = {
            title: (item.tag ? item.tag : 'No tag'),
            items: []
          };
          tag = item.tag;
          result.push(section);
        }
        section.items.push(item);
      }
      return result;
    };

    return Whiskey2;

  })();

  Notepad = (function() {

    Notepad.prototype.zoom = 1;

    Notepad.prototype.zoomStep = 0.1;

    function Notepad(app, notepad, div) {
      var _this = this;
      this.app = app;
      this.notepad = notepad;
      this.div = div;
      this.div.find('.notepad-name').text(this.notepad.name);
      this.divMiniatures = div.find('.notepad-sheets');
      this.divContent = div.find('.notepad-sheets-wrap');
      this.div.find('.notepad-add-sheet').bind('click', function() {
        return _this.showSheetDialog({}, function() {
          return _this.reloadSheets();
        });
      });
      this.div.find('.notepad-reload-sheets').bind('click', function() {
        return _this.reloadSheetsBookmarks();
      });
      this.div.find('.notepad-zoom-in').bind('click', function() {
        return _this.zoomInOut(1);
      });
      this.div.find('.notepad-zoom-out').bind('click', function() {
        return _this.zoomInOut(-1);
      });
      this.initSheetPlaceholders();
      this.reloadSheets();
    }

    Notepad.prototype.reloadSheetsBookmarks = function() {
      var _this = this;
      return this.app.refreshBookmarks(function() {
        return _this.reloadSheets();
      });
    };

    Notepad.prototype.zoomInOut = function(direction) {
      if (direction == null) direction = 1;
      if (direction < 0 && this.zoom <= this.zoomStep) return;
      this.zoom += direction * this.zoomStep;
      this.divContent.css({
        'font-size': "" + this.zoom + "em"
      });
      return this.reloadSheets();
    };

    Notepad.prototype.showBMarkDialog = function(bmark, handler) {
      var _ref, _ref2,
        _this = this;
      $('#bmark-dialog').modal('show');
      $('#bmark-dialog-name').val((_ref = bmark.name) != null ? _ref : 'Untitled').focus();
      $('#bmark-dialog-color').val((_ref2 = bmark.color) != null ? _ref2 : '#ff0000');
      $('#do-remove-bmark-dialog').unbind('click').bind('click', function(e) {
        _this.app.showPrompt('Are you sure want to remove bookmark?', function() {
          return _this.app.manager.removeCascade('bookmarks', bmark, function(err) {
            if (err) return _this.app.showError('Error removing bookmark');
            if (handler) return handler(bmark);
          });
        });
        return $('#bmark-dialog').modal('hide');
      });
      return $('#do-save-bmark-dialog').unbind('click').bind('click', function(e) {
        var color, name, _handler;
        name = $('#bmark-dialog-name').val().trim();
        color = $('#bmark-dialog-color').val();
        if (!name) return _this.app.showError('Name is empty');
        log('Save bmark', bmark, name, color);
        bmark.name = name;
        bmark.color = color;
        _handler = function(err, data) {
          if (err) return _this.app.showError(err);
          _this.app.showAlert('Bookmark saved', {
            severity: 'success'
          });
          $('#bmark-dialog').modal('hide');
          if (handler) return handler(bmark);
        };
        return _this.app.manager.save('bookmarks', bmark, _handler);
      });
    };

    Notepad.prototype.showSheetDialog = function(sheet, handler) {
      var noTemplateText, pluginsText, templateButtonText, templateID, ul, _ref, _ref2,
        _this = this;
      noTemplateText = 'No template';
      $('#sheet-dialog').modal('show');
      templateID = (_ref = sheet.template_id) != null ? _ref : null;
      $('#sheet-dialog-name').val((_ref2 = sheet.title) != null ? _ref2 : '').focus().select();
      pluginsText = $('#sheet-dialog-plugins');
      pluginsText.val(sheet.plugins ? sheet.plugins.join(',') : '');
      $('#do-remove-sheet-dialog').unbind('click').bind('click', function(e) {
        if (!sheet.id) return;
        _this.app.showPrompt('Are you sure want to remove sheet? It will remove notes also', function() {
          return _this.removeSheets([sheet.id]);
        });
        $('#sheet-dialog').modal('hide');
        return false;
      });
      $('#do-save-sheet-dialog').unbind('click').bind('click', function(e) {
        var item, name, pluginsArr, _handler, _i, _len;
        name = $('#sheet-dialog-name').val().trim();
        if (!name) return _this.app.showError('Title is empty');
        log('Save sheet', name, templateID);
        pluginsArr = pluginsText.val().split(',');
        sheet.plugins = [];
        for (_i = 0, _len = pluginsArr.length; _i < _len; _i++) {
          item = pluginsArr[_i];
          if (item.trim()) sheet.plugins.push(item.trim());
        }
        if (sheet.plugins.length === 0) delete sheet.plugins;
        sheet.template_id = templateID;
        sheet.title = name;
        _handler = function(err, data) {
          if (err) return _this.app.showError(err);
          _this.app.showAlert('Sheet saved', {
            severity: 'success'
          });
          $('#sheet-dialog').modal('hide');
          if (handler) return handler(sheet);
        };
        if (!sheet.id) {
          sheet.notepad_id = _this.notepad.id;
          _this.app.manager.storage.create('sheets', sheet, _handler);
        } else {
          _this.app.manager.storage.update('sheets', sheet, _handler);
        }
        return false;
      });
      templateButtonText = $('#sheet-dialog-template-title');
      templateButtonText.text(noTemplateText);
      ul = $('#sheet-dialog-template-menu').empty();
      return this.app.manager.storage.select('templates', [], function(err, data) {
        var a, item, li, section, ul2, _i, _len, _results;
        if (err) return;
        data = _this.app.sortTemplates(data);
        ul.empty();
        li = $(document.createElement('li')).appendTo(ul);
        a = $(document.createElement('a')).appendTo(li);
        a.text(noTemplateText);
        a.bind('click', function(e) {
          templateID = null;
          return templateButtonText.text(noTemplateText);
        });
        _results = [];
        for (_i = 0, _len = data.length; _i < _len; _i++) {
          section = data[_i];
          li = $(document.createElement('li')).addClass('dropdown-submenu').appendTo(ul);
          a = $(document.createElement('a')).appendTo(li);
          a.text(section.title);
          ul2 = $(document.createElement('ul')).addClass('dropdown-menu').appendTo(li);
          _results.push((function() {
            var _fn, _j, _len2, _ref3, _results2,
              _this = this;
            _ref3 = section.items;
            _fn = function(a, item) {
              return a.bind('click', function(e) {
                templateID = item.id;
                return templateButtonText.text(item.name);
              });
            };
            _results2 = [];
            for (_j = 0, _len2 = _ref3.length; _j < _len2; _j++) {
              item = _ref3[_j];
              li = $(document.createElement('li')).appendTo(ul2);
              a = $(document.createElement('a')).appendTo(li);
              a.text(item.name);
              _fn(a, item);
              if (templateID === item.id) {
                _results2.push(templateButtonText.text(item.name));
              } else {
                _results2.push(void 0);
              }
            }
            return _results2;
          }).call(_this));
        }
        return _results;
      }, {
        order: ['tag', 'name']
      });
    };

    Notepad.prototype.removeSheets = function(ids) {
      var config, id, item, items, sink, _i, _j, _len, _len2,
        _this = this;
      sink = [];
      items = [];
      config = [];
      for (_i = 0, _len = ids.length; _i < _len; _i++) {
        id = ids[_i];
        config.push({
          stream: 'sheets',
          type: 'removeCascade',
          object: {
            id: id
          }
        });
        items.push({
          id: id
        });
      }
      this.app.removeWithNext(sink, this.sheets, items);
      for (_j = 0, _len2 = sink.length; _j < _len2; _j++) {
        item = sink[_j];
        config.push({
          type: 'update',
          stream: 'sheets',
          object: item
        });
      }
      return this.app.manager.batch(config, function(err) {
        if (err) return _this.app.showError(err);
        return _this.reloadSheets();
      });
    };

    Notepad.prototype.moveSheets = function(index, data) {
      var _this = this;
      return this.app.moveWithNext('sheets', this.sheets, index, data.ids, function(err) {
        if (err) return _this.app.showError(err);
        return _this.reloadSheets();
      }, function(item) {
        return item.notepad_id = _this.notepad.id;
      });
    };

    Notepad.prototype.moveBookmark = function(id, sheet, handler) {
      var _this = this;
      return this.app.manager.findOne('bookmarks', id, function(err, bmark) {
        if (err) return _this.app.showError(err);
        bmark.sheet_id = sheet.id;
        bmark.notepad_id = _this.notepad.id;
        return _this.app.manager.save('bookmarks', bmark, function(err) {
          if (err) return _this.app.showError(err);
          return _this.reloadSheetsBookmarks();
        });
      });
    };

    Notepad.prototype.renderBookmark = function(bmark, zoom, handler) {
      var color, div, _ref,
        _this = this;
      color = (_ref = bmark.color) != null ? _ref : '#ff0000';
      div = $(document.createElement('div')).addClass('bookmark');
      div.css({
        'border-color': color,
        'border-bottom-color': 'transparent',
        'font-size': "" + zoom + "em"
      });
      div.attr({
        draggable: true,
        rel: 'tooltip',
        title: bmark.name
      });
      div.tooltip({
        placement: 'bottom'
      });
      div.bind('dblclick', function(e) {
        e.preventDefault();
        e.stopPropagation();
        if (handler) handler(bmark);
        return false;
      });
      div.bind('dragstart', function(e) {
        return _this.app.dragSetType(e, 'custom/bmark', {
          id: bmark.id
        });
      });
      return div;
    };

    Notepad.prototype.renderSheetMiniatures = function(sheets) {
      var div, divItem, divItemText, h, height, i, maxHeight, minHeight, oneItemHeight, renderBookmarks, sheet, step, _fn, _ref,
        _this = this;
      div = this.divMiniatures;
      div.empty();
      this.selectedSheets = {};
      if (!(sheets != null ? sheets.length : void 0)) return;
      minHeight = 3;
      maxHeight = 30;
      oneItemHeight = 50;
      height = $(window).height() - 180;
      step = maxHeight;
      if (sheets.length > 1) {
        step = Math.floor((height - oneItemHeight) / (sheets.length - 1));
      }
      if (step > maxHeight) step = maxHeight;
      if (step < minHeight) step = minHeight;
      h = 0;
      renderBookmarks = function(i, sheet, divItem) {
        var arr, bmarkstep, bmarkx, bmarky, divBMark, item, _i, _len, _results;
        arr = _this.app.bookmarks[sheet.id];
        if (!arr) return;
        bmarkx = 0;
        bmarky = 0;
        bmarkstep = 1;
        _results = [];
        for (_i = 0, _len = arr.length; _i < _len; _i++) {
          item = arr[_i];
          divBMark = _this.renderBookmark(item, 0.5, function(bmark) {
            return _this.showBMarkDialog(bmark, function() {
              return _this.reloadSheetsBookmarks();
            });
          });
          divBMark.css({
            top: "" + bmarkx + "em",
            right: "" + bmarky + "em"
          });
          divItem.append(divBMark);
          bmarkx += bmarkstep;
          _results.push(bmarky -= bmarkstep);
        }
        return _results;
      };
      _fn = function(i, sheet, divItem) {
        renderBookmarks(i, sheet, divItem);
        divItem.bind('click', function(e) {
          e.preventDefault();
          if (e.ctrlKey) {
            if (_this.selectedSheets[sheet.id]) {
              delete _this.selectedSheets[sheet.id];
              divItem.removeClass('notepad-sheet-miniature-selected');
            } else {
              _this.selectedSheets[sheet.id] = true;
              divItem.addClass('notepad-sheet-miniature-selected');
            }
            return;
          }
          div.find('.notepad-sheet-miniature-selected').removeClass('notepad-sheet-miniature-selected');
          _this.selectedSheets = {};
          return _this.loadSheets(i);
        });
        divItem.bind('dragstart', function(e) {
          var id, ids;
          if (_this.selectedSheets[sheet.id]) {
            ids = [];
            for (id in _this.selectedSheets) {
              ids.push(id);
            }
            return _this.app.dragSetType(e, 'custom/sheet', {
              ids: ids
            });
          } else {
            return _this.app.dragSetType(e, 'custom/sheet', {
              ids: [sheet.id]
            });
          }
        });
        divItem.bind('dragover', function(e) {
          if (_this.app.dragHasType(e, 'custom/sheet')) e.preventDefault();
          return false;
        });
        divItem.bind('drop', function(e) {
          var bmark, sheetsData;
          bmark = _this.app.dragGetType(e, 'custom/bmark');
          if (bmark) {
            _this.moveBookmark(bmark.id, sheet);
            e.stopPropagation();
            return false;
          }
          sheetsData = _this.app.dragGetType(e, 'custom/sheet');
          if (sheetsData) {
            _this.moveSheets(i, sheetsData);
            e.stopPropagation();
          }
          return false;
        });
        divItem.bind('mouseover', function(e) {
          return divItem.addClass('notepad-sheet-miniature-hover');
        });
        divItem.bind('dragenter', function(e) {
          divItem.addClass('notepad-sheet-miniature-hover');
          if (_this.app.dragHasType(e, 'custom/bmark')) e.preventDefault();
          if (_this.app.dragHasType(e, 'custom/note')) {
            _this.loadSheets(i);
            return e.preventDefault();
          }
        });
        divItem.bind('mouseout', function(e) {
          return divItem.removeClass('notepad-sheet-miniature-hover');
        });
        return divItem.bind('dragleave', function(e) {
          return divItem.removeClass('notepad-sheet-miniature-hover');
        });
      };
      for (i = 0, _ref = sheets.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        sheet = sheets[i];
        divItem = $(document.createElement('div')).addClass('notepad-sheet-miniature');
        divItem.attr({
          draggable: true
        });
        divItem.css({
          top: "" + h + "px"
        });
        divItem.appendTo(div);
        divItemText = $(document.createElement('div')).addClass('notepad-sheet-miniature-title').appendTo(divItem);
        divItemText.text(sheet.title);
        h += step;
        _fn(i, sheet, divItem);
      }
      return $(document.createElement('div')).addClass('clear').appendTo(div);
    };

    Notepad.prototype.reloadSheets = function() {
      var _this = this;
      return this.app.manager.storage.select('sheets', ['notepad_id', this.notepad.id], function(err, sheets) {
        var index;
        if (err) return _this.app.showError(err);
        _this.sheets = _this.app.sortWithNext(sheets);
        _this.renderSheetMiniatures(_this.sheets);
        index = _this.app.findByID(_this.sheets, _this.lastSheetID);
        if (index === -1 && _this.sheets.length > 0) index = 0;
        if (index !== -1) return _this.loadSheets(index);
      });
    };

    Notepad.prototype.maxSheetsVisible = 2;

    Notepad.prototype.initSheetPlaceholders = function() {
      var canvas, div, divContent, divTitle, divToolbar, i, _ref, _results;
      this.sheetDivs = [];
      _results = [];
      for (i = 0, _ref = this.maxSheetsVisible; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        div = $(document.createElement('div')).addClass('sheet').appendTo(this.divContent);
        divContent = $(document.createElement('div')).addClass('sheet_content').appendTo(div);
        canvas = $(document.createElement('canvas')).addClass('sheet-canvas').appendTo(divContent);
        divTitle = $(document.createElement('div')).addClass('sheet_title').appendTo(div);
        divToolbar = $('#sheet-toolbar-template').clone().removeClass('hide').appendTo(div);
        this.sheetDivs.push(div);
        div.hide();
        if (i === 0) {
          _results.push(this.spiralDiv = $(document.createElement('div')).addClass('sheet-spiral').appendTo(this.divContent));
        } else {
          _results.push(void 0);
        }
      }
      return _results;
    };

    Notepad.prototype.loadSheets = function(index) {
      var CENTER_SPIRAL, NO_SPIRAL, RIGHT_SPIRAL, count, div, sheetsVisible, spiralHeight, spiralType, tmpl, _ref;
      this.selectedNotes = {};
      this.lastSheetID = (_ref = this.sheets[index]) != null ? _ref.id : void 0;
      count = 0;
      this.notes = [];
      NO_SPIRAL = 0;
      RIGHT_SPIRAL = 1;
      CENTER_SPIRAL = 2;
      spiralType = NO_SPIRAL;
      spiralHeight = -1;
      sheetsVisible = 0;
      if (index + this.maxSheetsVisible >= this.sheets.length) {
        index = this.sheets.length - this.maxSheetsVisible;
        if (index < 0) index = 0;
      }
      while (count < this.maxSheetsVisible) {
        div = this.sheetDivs[count];
        if (!this.sheets[index]) {
          div.hide();
        } else {
          div.show();
          sheetsVisible++;
          tmpl = this.loadSheet(index, div);
          if (div.height() < spiralHeight || spiralHeight < 0) {
            spiralHeight = div.height();
          }
        }
        index++;
        count++;
      }
      if (sheetsVisible === 1) {
        spiralType = RIGHT_SPIRAL;
      } else if (sheetsVisible > 1) {
        spiralType = CENTER_SPIRAL;
      }
      spiralHeight = "" + (Math.floor(spiralHeight / 18) * 18) + "px";
      switch (spiralType) {
        case NO_SPIRAL:
          return this.spiralDiv.hide();
        case RIGHT_SPIRAL:
          return this.spiralDiv.show().height(spiralHeight).attr({
            'class': 'sheet-spiral spiral-right'
          });
        case CENTER_SPIRAL:
          return this.spiralDiv.show().height(spiralHeight).attr({
            'class': 'sheet-spiral spiral-center'
          });
      }
    };

    Notepad.prototype.showNotesDialog = function(sheet, note, handler) {
      var a, adiv, color, colors, currentColor, currentText, currentWidth, width, widths, _i, _len, _ref, _ref2, _ref3, _ref4, _ref5, _ref6, _ref7,
        _this = this;
      $('#note-dialog').modal({
        backdrop: 'static',
        keyboard: false
      });
      currentText = (_ref = note.text) != null ? _ref : '';
      $('#note-dialog-text').val(currentText).focus();
      $('#note-dialog-collapsed').attr({
        'checked': note.collapsed ? true : false
      });
      colors = $('#note-dialog-colors').empty();
      widths = $('#note-dialog-widths').empty();
      currentColor = (_ref2 = note.color) != null ? _ref2 : (_ref3 = this.lastColor) != null ? _ref3 : 0;
      currentWidth = (_ref4 = note.width) != null ? _ref4 : (_ref5 = this.lastWidth) != null ? _ref5 : this.noteWidths[this.noteDefaultWidth];
      for (color = 0, _ref6 = this.colors; 0 <= _ref6 ? color < _ref6 : color > _ref6; 0 <= _ref6 ? color++ : color--) {
        a = $(document.createElement('a')).addClass('btn btn-small').attr({
          href: '#'
        }).data('index', color);
        a.appendTo(colors);
        if (currentColor === color) a.addClass('active');
        adiv = $(document.createElement('div')).addClass('note-color-button note-color' + color).html('&nbsp;').appendTo(a);
      }
      colors.button();
      _ref7 = this.noteWidths;
      for (_i = 0, _len = _ref7.length; _i < _len; _i++) {
        width = _ref7[_i];
        a = $(document.createElement('a')).addClass('btn').attr({
          href: '#'
        }).data('width', width);
        a.text("" + width + "%").appendTo(widths);
        if (currentWidth === width) a.addClass('active');
      }
      widths.button();
      $('#do-close-note-dialog').unbind('click').bind('click', function(e) {
        var text;
        text = $('#note-dialog-text').val().trim();
        if (text !== currentText) {
          return _this.app.showPrompt('There is unsaved text. Are you sure want to close dialog?', function() {
            return $('#note-dialog').modal('hide');
          });
        } else {
          return $('#note-dialog').modal('hide');
        }
      });
      $('#do-remove-note-dialog').unbind('click').bind('click', function(e) {
        if (!note.id) return;
        return _this.app.showPrompt('Are you sure want to remove note?', function() {
          return _this.app.manager.storage.remove('notes', note, function(err) {
            if (err) return _this.app.showError(err);
            $('#note-dialog').modal('hide');
            return _this.reloadSheets();
          });
        });
      });
      return $('#do-save-note-dialog').unbind('click').bind('click', function(e) {
        var collapsed, text, _handler;
        text = $('#note-dialog-text').val().trim();
        collapsed = $('#note-dialog-collapsed').attr('checked');
        color = colors.find('a.active').data('index');
        width = widths.find('a.active').data('width');
        log('Save', collapsed, color, width);
        if (!text) return _this.app.showError('Text is empty');
        note.text = text;
        note.color = color;
        note.width = width;
        note.collapsed = collapsed ? true : false;
        _handler = function(err, data) {
          if (err) return _this.app.showError(err);
          _this.app.showAlert('Note saved', {
            severity: 'success'
          });
          $('#note-dialog').modal('hide');
          if (handler) return handler(note);
        };
        if (!note.id) {
          _this.lastColor = note.color;
          _this.lastWidth = note.width;
          note.sheet_id = sheet.id;
          return _this.app.manager.storage.create('notes', note, _handler);
        } else {
          return _this.app.manager.storage.update('notes', note, _handler);
        }
      });
    };

    Notepad.prototype.resetNoteSelection = function() {
      this.selectedNotes = {};
      return this.divContent.find('.note-selected').removeClass('note-selected');
    };

    Notepad.prototype.preciseEm = function(value) {
      return Math.floor(value * 100 / this.zoomFactor) / 100;
    };

    Notepad.prototype.createSpotLink = function(id, spot) {
      var _this = this;
      this.app.manager.findOne('notes', id, function(err, note) {
        var index, links;
        if (err) return _this.app.showError(err);
        links = note.links;
        if (!links) {
          note.links = [];
          links = note.links;
        }
        index = _this.app.findByID(links, spot.id, 'spot');
        if (index !== -1) {
          log('Spot already created');
          return;
        }
        links.push({
          spot: spot.id
        });
        return _this.app.manager.save('notes', note, function(err) {
          if (err) return _this.app.showError(err);
          return _this.reloadSheets();
        });
      });
      return true;
    };

    Notepad.prototype.createNoteLink = function(note, noteID) {
      var index, links, otherNote,
        _this = this;
      if (note.id === noteID) {
        log('Same note');
        return false;
      }
      index = this.app.findByID(this.notes, noteID);
      if (-1 === index) {
        log('From other page');
        return false;
      }
      otherNote = this.notes[index];
      if (note.sheet_id !== otherNote.sheet_id) {
        log('From other sheet');
        return false;
      }
      links = otherNote.links;
      if (!links) {
        otherNote.links = [];
        links = otherNote.links;
      }
      index = this.app.findByID(links, note.id);
      if (index !== -1) {
        log('Already created');
        return false;
      }
      links.push({
        id: note.id
      });
      this.app.manager.save('notes', otherNote, function(err) {
        if (err) return _this.app.showError(err);
        return _this.reloadSheets();
      });
      return true;
    };

    Notepad.prototype.loadNotes = function(sheet, parent, canvas, zoom) {
      var createSpots, loadNote, renderArrow, renderBookmarks, renderLinks,
        _this = this;
      createSpots = function(note) {
        var links, spot, spotDiv, spots, x, y, _i, _len, _ref, _ref2, _ref3, _results;
        spots = (_ref = (_ref2 = sheet.config) != null ? _ref2.spots : void 0) != null ? _ref : [];
        links = (_ref3 = note.links) != null ? _ref3 : [];
        _results = [];
        for (_i = 0, _len = spots.length; _i < _len; _i++) {
          spot = spots[_i];
          spotDiv = $(document.createElement('div')).addClass('note-spot').appendTo(parent);
          x = _this.preciseEm(spot.x);
          y = _this.preciseEm(spot.y);
          spotDiv.css({
            left: "" + x + "em",
            top: "" + y + "em"
          });
          _results.push((function(spot, spotDiv) {
            spotDiv.bind('dragover', function(e) {
              if (_this.app.dragHasType(e, 'custom/note')) {
                spotDiv.addClass('note-spot-hover');
                return e.preventDefault();
              }
            });
            spotDiv.bind('drop', function(e) {
              var noteData;
              noteData = _this.app.dragGetType(e, 'custom/note');
              if ((noteData != null ? noteData.id : void 0) && (noteData != null ? noteData.link : void 0)) {
                if (_this.createSpotLink(noteData.id, spot)) {
                  e.stopPropagation();
                  return false;
                }
              }
            });
            spotDiv.bind('dragleave', function(e) {
              return spotDiv.removeClass('note-spot-hover');
            });
            return spotDiv.bind('dragend', function(e) {
              return spotDiv.remove();
            });
          })(spot, spotDiv));
        }
        return _results;
      };
      renderArrow = function(box1, box2, color) {
        var difx, dify, triangleSize, verticalTriangle, x1, x2, xt1, xt2, y1, y2, yt1, yt2;
        triangleSize = 2 * zoom;
        x1 = box1.x + box1.w / 2;
        x2 = box2.x + box2.w / 2;
        y1 = box1.y + box1.h / 2;
        y2 = box2.y + box2.h / 2;
        difx = Math.abs(x2 - x1);
        dify = Math.abs(y2 - y1);
        verticalTriangle = difx > dify;
        if (verticalTriangle) {
          xt1 = x1;
          xt2 = x1;
          yt1 = y1 - triangleSize;
          yt2 = y1 + triangleSize;
        } else {
          xt1 = x1 - triangleSize;
          xt2 = x1 + triangleSize;
          yt1 = y1;
          yt2 = y1;
        }
        return canvas.beginPath().moveTo(xt1, yt1).lineTo(xt2, yt2).lineTo(x2, y2).fill({
          fillStyle: color
        }).endPath();
      };
      renderLinks = function(notes) {
        var box1, box2, createLink, div2box, dotDiv, dotsDiv, dotsRadius, i, index, link, links, note, other, radius, spot, spotDiv, spotIndex, spots, x, y, _i, _j, _len, _len2, _ref, _ref2, _ref3, _ref4, _ref5, _results;
        dotsRadius = 2;
        spots = (_ref = (_ref2 = sheet.config) != null ? _ref2.spots : void 0) != null ? _ref : [];
        for (_i = 0, _len = spots.length; _i < _len; _i++) {
          spot = spots[_i];
          spotDiv = $(document.createElement('div')).addClass('note-spot-place').appendTo(parent);
          spotDiv.attr({
            id: "spot" + sheet.id + spot.id
          });
          x = _this.preciseEm(spot.x);
          y = _this.preciseEm(spot.y);
          spotDiv.css({
            left: "" + x + "em",
            top: "" + y + "em"
          });
        }
        _results = [];
        for (_j = 0, _len2 = notes.length; _j < _len2; _j++) {
          note = notes[_j];
          links = (_ref3 = note.links) != null ? _ref3 : [];
          if (links.length === 0) continue;
          dotsDiv = $(document.createElement('div')).addClass('note-dots').appendTo(parent);
          x = _this.preciseEm(note.x - 2 * dotsRadius);
          y = _this.preciseEm(note.y);
          radius = _this.preciseEm(dotsRadius);
          dotsDiv.css({
            left: "" + x + "em",
            top: "" + y + "em"
          });
          spots = (_ref4 = (_ref5 = sheet.config) != null ? _ref5.spots : void 0) != null ? _ref4 : [];
          div2box = function(div) {
            return {
              x: div.position().left,
              y: div.position().top,
              w: div.outerWidth(),
              h: div.outerHeight()
            };
          };
          box1 = div2box($('#note' + note.id));
          _results.push((function() {
            var _ref6, _results2,
              _this = this;
            _results2 = [];
            for (i = 0, _ref6 = links.length; 0 <= _ref6 ? i < _ref6 : i > _ref6; 0 <= _ref6 ? i++ : i--) {
              link = links[i];
              index = this.app.findByID(notes, link.id);
              createLink = true;
              other = null;
              if (index === -1) {
                createLink = false;
                if (link.spot) {
                  spotIndex = this.app.findByID(spots, link.spot);
                  if (spotIndex !== -1) {
                    spot = spots[spotIndex];
                    createLink = true;
                    box2 = div2box($("#spot" + sheet.id + spot.id));
                  }
                }
              } else {
                other = notes[index];
                if (other.sheet_id !== note.sheet_id) {
                  createLink = false;
                } else {
                  box2 = div2box($('#note' + other.id));
                }
              }
              if (createLink) renderArrow(box1, box2, '#ffaaaa', 6);
              dotDiv = $(document.createElement('div')).addClass('note-dot').appendTo(dotsDiv);
              dotDiv.css({
                'border-width': "" + radius + "em",
                'border-radius': "" + radius + "em"
              });
              dotDiv.css({
                'border-color': (createLink ? '#008800' : '#ff0000')
              });
              _results2.push((function(i, link, note) {
                return dotDiv.bind('dblclick', function(e) {
                  _this.app.showPrompt('Are you sure want to remove link?', function() {
                    note.links.splice(i, 1);
                    return _this.app.manager.save('notes', note, function(err) {
                      if (err) return _this.app.showError(err);
                      return _this.reloadSheets();
                    });
                  });
                  e.stopPropagation();
                  e.preventDefault();
                  return false;
                });
              })(i, link, note));
            }
            return _results2;
          }).call(_this));
        }
        return _results;
      };
      loadNote = function(note) {
        var div, fi, files, i, line, lines, mergeNotes, notewidth, renderIcon, resizer, width, x, y, _ref, _ref2, _ref3, _ref4, _ref5, _ref6, _ref7, _ref8;
        mergeNotes = function(ids) {
          var config, id, _i, _len;
          config = [
            {
              type: 'findOne',
              id: note.id,
              stream: 'notes'
            }
          ];
          for (_i = 0, _len = ids.length; _i < _len; _i++) {
            id = ids[_i];
            config.push({
              type: 'findOne',
              id: id,
              stream: 'notes'
            });
          }
          return _this.app.manager.batch(config, function(err, arr) {
            var i, n, nn, _ref;
            if (err) return _this.app.showError(err);
            n = arr[0];
            for (i = 1, _ref = arr.length; 1 <= _ref ? i < _ref : i > _ref; 1 <= _ref ? i++ : i--) {
              nn = arr[i];
              if (nn.text) n.text += '\n> ' + nn.text;
            }
            return _this.app.manager.save('notes', n, function(err) {
              if (err) return _this.app.showError(err);
              return _this.reloadSheets();
            });
          });
        };
        div = $(document.createElement('div')).addClass('note').appendTo(parent);
        div.attr({
          draggable: true,
          id: 'note' + note.id
        });
        div.bind('click', function(e) {
          e.preventDefault();
          if (e.ctrlKey) {
            if (_this.selectedNotes[note.id]) {
              delete _this.selectedNotes[note.id];
              return div.removeClass('note-selected');
            } else {
              _this.selectedNotes[note.id] = true;
              return div.addClass('note-selected');
            }
          } else {
            return _this.resetNoteSelection();
          }
        });
        div.bind('dragstart', function(e) {
          var id, ids, offset;
          offset = _this.app.dragGetOffset(e, div);
          if (_this.selectedNotes[note.id]) {
            ids = [];
            for (id in _this.selectedNotes) {
              ids.push(id);
            }
            return _this.app.dragSetType(e, 'custom/note', {
              ids: ids,
              x: offset.left,
              y: offset.top,
              append: e.shiftKey
            });
          } else {
            if (e.ctrlKey) createSpots(note);
            return _this.app.dragSetType(e, 'custom/note', {
              id: note.id,
              x: offset.left,
              y: offset.top,
              link: e.ctrlKey,
              append: e.shiftKey
            });
          }
        });
        div.bind('dragover', function(e) {
          if ((_this.app.dragHasType(e, 'custom/note')) || (_this.app.dragHasType(e, 'Files'))) {
            return e.preventDefault();
          }
        });
        div.bind('drop', function(e) {
          var files, noteData;
          noteData = _this.app.dragGetType(e, 'custom/note');
          if ((noteData != null ? noteData.id : void 0) && (noteData != null ? noteData.link : void 0)) {
            log('Dropped note:', noteData);
            if (_this.createNoteLink(note, noteData.id)) {
              e.stopPropagation();
              return false;
            }
          }
          if (noteData != null ? noteData.append : void 0) {
            mergeNotes(noteData.id ? [noteData.id] : noteData.ids);
            e.stopPropagation();
            return false;
          }
          files = _this.app.dragGetType(e, 'Files');
          if (files && files.length > 0) {
            return _this.app.manager.storage.uploadFile(files[0], function(err, name) {
              if (err) return _this.app.showError(err);
              if (!note.files) note.files = [];
              note.files.push(name);
              return _this.app.manager.save('notes', note, function(err) {
                if (err) return _this.app.showError(err);
                log('Note saved with attachment');
                return _this.reloadSheets();
              });
            });
          }
        });
        div.bind('dblclick', function(e) {
          _this.showNotesDialog(sheet, note, function() {
            return _this.reloadSheets();
          });
          e.preventDefault();
          return e.stopPropagation();
        });
        div.addClass('note-color0 note-color' + ((_ref = note.color) != null ? _ref : 0));
        notewidth = (_ref2 = note.width) != null ? _ref2 : _this.noteWidths[_this.noteDefaultWidth];
        width = _this.preciseEm(notewidth);
        x = _this.preciseEm((_ref3 = note.x) != null ? _ref3 : 0);
        y = _this.preciseEm((_ref4 = note.y) != null ? _ref4 : 0);
        div.css({
          width: "" + width + "em",
          left: "" + x + "em",
          top: "" + y + "em"
        });
        files = (_ref5 = note.files) != null ? _ref5 : [];
        if (note.collapsed) div.addClass('note-collapsed');
        renderIcon = function(fi) {
          var ICON_SIZE, attDiv, file, iconsize, iconsizeexpanded, img, span;
          ICON_SIZE = 12;
          file = files[fi];
          attDiv = $(document.createElement('div')).addClass('note-file').appendTo(div);
          iconsize = _this.preciseEm(ICON_SIZE);
          iconsizeexpanded = notewidth - 8;
          attDiv.css({
            width: "" + iconsize + "em",
            height: "" + iconsize + "em"
          });
          if (_.endsWith(file, '.jpg')) {
            attDiv.addClass('note-file-image');
            img = $(document.createElement('img'));
            img.css({
              visibility: 'hidden'
            });
            img.bind('load', function() {
              var expheight, expwidth, iconheight, iconwidth, mul;
              mul = img.width() / iconsizeexpanded;
              expwidth = _this.preciseEm(iconsizeexpanded);
              expheight = _this.preciseEm(img.height() / mul);
              mul = Math.max(img.width(), img.height()) / ICON_SIZE;
              iconwidth = iconsize;
              iconheight = iconwidth;
              div.bind('mouseover', function() {
                attDiv.css({
                  width: "" + expwidth + "em",
                  height: "" + expheight + "em"
                });
                return attDiv.addClass('note-file-image-hover');
              });
              div.bind('mouseout', function() {
                attDiv.css({
                  width: "" + iconwidth + "em",
                  height: "" + iconheight + "em"
                });
                return attDiv.removeClass('note-file-image-hover');
              });
              attDiv.css({
                width: "" + iconwidth + "em",
                height: "" + iconheight + "em"
              });
              img.addClass('note-image');
              return img.css({
                visibility: 'visible'
              });
            });
            _this.app.manager.storage.getFile(file, function(err, link) {
              if (!err) {
                img.attr('src', link);
                return img.appendTo(attDiv);
              }
            });
          } else {
            attDiv.addClass('note-file-other');
            span = $(document.createElement('span')).addClass('note-file-other-text').appendTo(attDiv);
            span.text(file.substr(file.lastIndexOf('.')));
          }
          return attDiv.bind('dblclick', function(e) {
            e.stopPropagation();
            e.preventDefault();
            _this.app.showPrompt('Are you sure want to delete file ' + file + '?', function() {
              return _this.app.manager.storage.removeFile(file, function(err) {
                if (err) return _this.app.showError(err);
                note.files.splice(fi, 1);
                return _this.app.manager.save('notes', note, function(err) {
                  if (err) return _this.app.showError(err);
                  return _this.reloadSheets();
                });
              });
            });
            return false;
          });
        };
        for (fi = 0, _ref6 = files.length; 0 <= _ref6 ? fi < _ref6 : fi > _ref6; 0 <= _ref6 ? fi++ : fi--) {
          renderIcon(fi);
        }
        lines = ((_ref7 = note.text) != null ? _ref7 : '').split('\n');
        for (i = 0, _ref8 = lines.length; 0 <= _ref8 ? i < _ref8 : i > _ref8; 0 <= _ref8 ? i++ : i--) {
          line = lines[i];
          if (!line) {
            if (i < lines.length - 1) {
              $(document.createElement('div')).addClass('note-br').html('&nbsp;').appendTo(div);
            }
          } else {
            $(document.createElement('div')).addClass('note-line').appendTo(div).text(line);
          }
        }
        resizer = $(document.createElement('div')).addClass('note-resizer').appendTo(div);
        resizer.attr({
          draggable: true
        });
        return resizer.bind('dragstart', function(e) {
          _this.app.dragSetType(e, 'custom/note-resize', {
            id: note.id
          });
          return e.stopPropagation();
        });
      };
      renderBookmarks = function(divItem) {
        var arr, bmarkstep, bmarkx, bmarky, divBMark, item, _i, _len, _results;
        arr = _this.app.bookmarks[sheet.id];
        if (!arr) return;
        bmarkx = 0.5;
        bmarky = 0;
        bmarkstep = 2.5;
        _results = [];
        for (_i = 0, _len = arr.length; _i < _len; _i++) {
          item = arr[_i];
          divBMark = _this.renderBookmark(item, 0.6, function(bmark) {
            return _this.showBMarkDialog(bmark, function() {
              return _this.reloadSheetsBookmarks();
            });
          });
          divBMark.css({
            top: "" + bmarky + "em",
            right: "" + bmarkx + "em"
          });
          divItem.append(divBMark);
          _results.push(bmarkx += bmarkstep);
        }
        return _results;
      };
      return this.app.manager.storage.select('notes', ['sheet_id', sheet.id], function(err, arr) {
        var item, _i, _len;
        if (err) return _this.app.showError(err);
        arr = arr.sort(function(a, b) {
          if (a.y < b.y) return -1;
          if (a.y > b.y) return 1;
          if (a.x < b.x) return -1;
          if (a.x > b.x) return 1;
          return 0;
        });
        parent.children('div').remove();
        for (_i = 0, _len = arr.length; _i < _len; _i++) {
          item = arr[_i];
          loadNote(item);
          _this.notes.push(item);
        }
        renderLinks(arr);
        return renderBookmarks(parent);
      });
    };

    Notepad.prototype.noteWidths = [50, 75, 90, 120, 150];

    Notepad.prototype.noteDefaultWidth = 1;

    Notepad.prototype.zoomFactor = 5;

    Notepad.prototype.colors = 8;

    Notepad.prototype.gridStep = 3;

    Notepad.prototype.stick = true;

    Notepad.prototype.loadSheet = function(index, div) {
      var bounds, canvas, clearSelector, conf, configButton, divContent, divTitle, height, inRectangle, key, notesInRectangle, offsetToCoordinates, plugins, sheet, sheetHeight, sheetWidth, stickToGrid, template, templateConfig, width, zoom, _i, _j, _len, _len2, _ref, _ref2, _ref3, _ref4, _ref5, _ref6,
        _this = this;
      clearSelector = function() {
        if (_this.selectorDiv) {
          _this.selectorDiv.remove();
          return _this.selectorDiv = null;
        }
      };
      clearSelector();
      inRectangle = function(x1, y1, x2, y2, x3, y3, x4, y4) {
        var x_overlap, y_overlap;
        x_overlap = Math.min(x2, x4) - Math.max(x1, x3);
        y_overlap = Math.min(y2, y4) - Math.max(y1, y3);
        if (x_overlap >= 0 && y_overlap >= 0) {
          return true;
        } else {
          return false;
        }
      };
      notesInRectangle = function(x1, y1, x2, y2) {
        var height, note, noteDiv, notes, width, _i, _len, _ref, _ref2, _ref3, _ref4, _ref5;
        if (x1 > x2) _ref = [x2, x1], x1 = _ref[0], x2 = _ref[1];
        if (y1 > y2) _ref2 = [y2, y1], y1 = _ref2[0], y2 = _ref2[1];
        notes = [];
        _ref4 = (_ref3 = _this.notes) != null ? _ref3 : [];
        for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
          note = _ref4[_i];
          noteDiv = divContent.children('#note' + note.id);
          if (noteDiv.size() !== 1) continue;
          _ref5 = offsetToCoordinates(noteDiv.outerWidth(), noteDiv.outerHeight()), width = _ref5[0], height = _ref5[1];
          if (inRectangle(x1, y1, x2, y2, note.x, note.y, note.x + width, note.y + height)) {
            notes.push(note);
          }
        }
        return [
          {
            x: x1,
            y: y1,
            width: x2 - x1,
            height: y2 - y1
          }, notes
        ];
      };
      offsetToCoordinates = function(x, y) {
        return [Math.floor(x / divContent.width() * sheetWidth), Math.floor(y / divContent.height() * sheetHeight)];
      };
      stickToGrid = function(x, y) {
        if (_this.stick) {
          return [Math.round(x / _this.gridStep) * _this.gridStep, Math.round(y / _this.gridStep) * _this.gridStep];
        } else {
          return [x, y];
        }
      };
      sheet = this.sheets[index];
      if (!sheet) return null;
      template = this.app.getTemplate(sheet.template_id);
      templateConfig = this.app.getTemplateConfig(template);
      sheetWidth = (_ref = sheet != null ? (_ref2 = sheet.config) != null ? _ref2.width : void 0 : void 0) != null ? _ref : template.width;
      sheetHeight = (_ref3 = sheet != null ? (_ref4 = sheet.config) != null ? _ref4.height : void 0 : void 0) != null ? _ref3 : template.height;
      divContent = div.find('.sheet_content');
      canvas = canto(div.find('.sheet-canvas').get(0));
      canvas.reset();
      width = this.preciseEm(sheetWidth);
      height = this.preciseEm(sheetHeight);
      divContent.css({
        width: "" + width + "em",
        height: "" + height + "em"
      });
      divTitle = div.find('.sheet_title');
      divTitle.text((_ref5 = sheet.title) != null ? _ref5 : 'Untitled');
      div.find('.sheet-toolbar-custom').empty();
      plugins = (_ref6 = sheet.plugins) != null ? _ref6 : [];
      for (_i = 0, _len = plugins.length; _i < _len; _i++) {
        key = plugins[_i];
        if (this.app.sheetPlugins[key]) {
          this.app.sheetPlugins[key].load(div, sheet, this);
        }
      }
      div.find('.sheet-toolbar-edit').unbind('click').bind('click', function() {
        return _this.showSheetDialog(sheet, function() {
          return _this.reloadSheets();
        });
      });
      div.find('.sheet-toolbar-add-bmark').unbind('click').bind('click', function() {
        return _this.showBMarkDialog({
          sheet_id: sheet.id,
          notepad_id: _this.notepad.id
        }, function() {
          return _this.reloadSheetsBookmarks();
        });
      });
      configButton = div.find('.sheet-toolbar-config');
      if (templateConfig) {
        configButton.removeClass('disabled').unbind('click').bind('click', function() {
          return templateConfig.configure(template, sheet, _this);
        });
      } else {
        configButton.addClass('disabled');
      }
      divContent.unbind();
      divContent.bind('mousedown', function(e) {
        var coords, notes, offset, x, y, _ref7, _ref8;
        offset = _this.app.dragGetOffset(e, divContent);
        _ref7 = offsetToCoordinates(offset.left, offset.top), x = _ref7[0], y = _ref7[1];
        _ref8 = notesInRectangle(x, y, x, y), coords = _ref8[0], notes = _ref8[1];
        if (notes.length === 0 && e.ctrlKey) {
          _this.selectorDiv = $(document.createElement('div')).appendTo(divContent).addClass('notes-selector');
          _this.selectorDiv.css({
            left: '' + _this.preciseEm(x) + 'em',
            top: '' + _this.preciseEm(y) + 'em'
          });
          _this.selectorX = x;
          _this.selectorY = y;
          _this.selectorIndex = index;
          return false;
        }
      });
      divContent.bind('mouseup', function(e) {
        var coords, note, notes, offset, x, y, _j, _len2, _ref7, _ref8;
        if (!e.ctrlKey && _this.selectorDiv) {
          clearSelector();
          return;
        }
        if (_this.selectorDiv && _this.selectorIndex === index) {
          offset = _this.app.dragGetOffset(e, divContent);
          _ref7 = offsetToCoordinates(offset.left, offset.top), x = _ref7[0], y = _ref7[1];
          _ref8 = notesInRectangle(x, y, _this.selectorX, _this.selectorY), coords = _ref8[0], notes = _ref8[1];
          for (_j = 0, _len2 = notes.length; _j < _len2; _j++) {
            note = notes[_j];
            if (_this.selectedNotes[note.id]) {
              delete _this.selectedNotes[note.id];
              divContent.children('#note' + note.id).removeClass('note-selected');
            } else {
              _this.selectedNotes[note.id] = true;
              divContent.children('#note' + note.id).addClass('note-selected');
            }
          }
          return clearSelector();
        }
      });
      divContent.bind('mousemove', function(e) {
        var coords, notes, offset, x, y, _ref7, _ref8;
        if (!e.ctrlKey && _this.selectorDiv) {
          clearSelector();
          return;
        }
        if (_this.selectorDiv && _this.selectorIndex === index) {
          offset = _this.app.dragGetOffset(e, divContent);
          _ref7 = offsetToCoordinates(offset.left, offset.top), x = _ref7[0], y = _ref7[1];
          _ref8 = notesInRectangle(x, y, _this.selectorX, _this.selectorY), coords = _ref8[0], notes = _ref8[1];
          return _this.selectorDiv.css({
            left: '' + _this.preciseEm(coords.x) + 'em',
            top: '' + _this.preciseEm(coords.y) + 'em',
            width: '' + _this.preciseEm(coords.width) + 'em',
            height: '' + _this.preciseEm(coords.height) + 'em'
          });
        }
      });
      divContent.bind('dblclick', function(e) {
        var x, y, _ref7, _ref8;
        _ref7 = offsetToCoordinates(e.offsetX, e.offsetY), x = _ref7[0], y = _ref7[1];
        _ref8 = stickToGrid(x, y), x = _ref8[0], y = _ref8[1];
        _this.showNotesDialog(sheet, {
          x: x,
          y: y
        }, function() {
          return _this.reloadSheets();
        });
        return e.preventDefault();
      });
      divContent.bind('dragover', function(e) {
        if (_this.app.dragHasType(e, 'custom/note-resize')) e.preventDefault();
        if (_this.app.dragHasType(e, 'custom/note')) e.preventDefault();
        if (_this.app.dragHasType(e, 'custom/bmark')) e.preventDefault();
        return false;
      });
      divContent.bind('drop', function(e) {
        var bmark, config, id, offset, otherNote, resizer, x, y, _j, _len2, _ref10, _ref7, _ref8, _ref9;
        bmark = _this.app.dragGetType(e, 'custom/bmark');
        if (bmark) {
          _this.moveBookmark(bmark.id, sheet);
          e.stopPropagation();
          return false;
        }
        resizer = _this.app.dragGetType(e, 'custom/note-resize');
        if (resizer) {
          offset = _this.app.dragGetOffset(e, divContent);
          _ref7 = offsetToCoordinates(offset.left, offset.top), x = _ref7[0], y = _ref7[1];
          _this.app.manager.findOne('notes', resizer.id, function(err, note) {
            var newWidth, w, _j, _len2, _ref8;
            if (err) return _this.app.showError(err);
            width = Math.max(_this.noteWidths[0], x - note.x);
            newWidth = width;
            _ref8 = _this.noteWidths;
            for (_j = 0, _len2 = _ref8.length; _j < _len2; _j++) {
              w = _ref8[_j];
              if (w <= width) newWidth = w;
            }
            note.width = newWidth;
            return _this.app.manager.save('notes', note, function(err) {
              if (err) return _this.app.showError(err);
              return _this.reloadSheets();
            });
          });
          e.stopPropagation();
          return false;
        }
        if (_this.app.dragHasType(e, 'custom/note')) {
          otherNote = _this.app.dragGetType(e, 'custom/note');
          offset = _this.app.dragGetOffset(e, divContent);
          _ref8 = offsetToCoordinates(offset.left - otherNote.x, offset.top - otherNote.y), x = _ref8[0], y = _ref8[1];
          _ref9 = stickToGrid(x, y), x = _ref9[0], y = _ref9[1];
          if (otherNote.id) {
            _this.app.manager.findOne('notes', otherNote != null ? otherNote.id : void 0, function(err, note) {
              if (err) return _this.app.showError(err);
              note.x = x;
              note.y = y;
              note.sheet_id = sheet.id;
              return _this.app.manager.storage.update('notes', note, function(err) {
                if (err) return _this.app.showError(err);
                return _this.reloadSheets();
              });
            });
          } else {
            config = [];
            _ref10 = otherNote.ids;
            for (_j = 0, _len2 = _ref10.length; _j < _len2; _j++) {
              id = _ref10[_j];
              config.push({
                type: 'findOne',
                id: id,
                stream: 'notes'
              });
            }
            _this.app.manager.batch(config, function(err, arr) {
              var index, moveX, moveY, note, updates, _ref11;
              if (err) return _this.app.showError(err);
              updates = [];
              moveX = 0;
              moveY = 0;
              arr = arr.sort(function(a, b) {
                if (a.y < b.y) return -1;
                if (a.y > b.y) return 1;
                return a.x - b.x;
              });
              for (index = 0, _ref11 = arr.length; 0 <= _ref11 ? index < _ref11 : index > _ref11; 0 <= _ref11 ? index++ : index--) {
                note = arr[index];
                if (index === 0) {
                  moveX = note.x - x;
                  moveY = note.y - y;
                }
                note.sheet_id = sheet.id;
                note.x -= moveX;
                note.y -= moveY;
                updates.push({
                  type: 'update',
                  stream: 'notes',
                  object: note
                });
              }
              return _this.app.manager.batch(updates, function(err) {
                if (err) return _this.app.showError(err);
                return _this.reloadSheets();
              });
            });
          }
        }
        return false;
      });
      canvas.width = divContent.width();
      canvas.height = divContent.height();
      zoom = divContent.width() / sheetWidth;
      bounds = {
        width: sheetWidth,
        height: sheetHeight
      };
      this.app.templateDrawer.render(template, sheet, canvas, zoom, bounds);
      for (_j = 0, _len2 = plugins.length; _j < _len2; _j++) {
        key = plugins[_j];
        if (this.app.sheetPlugins[key]) {
          conf = this.app.sheetPlugins[key].getConfig(sheet.config);
          log('Draw:', sheet.config, conf, key);
          if (conf != null ? conf.draw : void 0) {
            this.app.templateDrawer.draw(conf.draw, canvas, zoom, bounds);
          }
        }
      }
      this.loadNotes(sheet, divContent, canvas, zoom);
      return template;
    };

    return Notepad;

  })();

  TemplateManager = (function() {

    function TemplateManager(app) {
      var _this = this;
      this.app = app;
      $('.templates-add').bind('click', function() {
        _this.edit();
        return false;
      });
      $('.template-save').bind('click', function() {
        _this.save();
        return false;
      });
      $('.template-remove').bind('click', function() {
        _this.remove();
        return false;
      });
      $('.template-clone').bind('click', function() {
        _this.clone();
        return false;
      });
      this.editName = $('#template-name');
      this.editTag = $('#template-tag');
      this.editWidth = $('#template-width');
      this.editHeight = $('#template-height');
      this.editType = $('#template-type');
      this.editConfig = $('#template-config');
      this.disableForm();
    }

    TemplateManager.prototype.disableForm = function() {
      var form;
      form = $('.template-form');
      form.find('input, button, textarea').attr({
        disabled: 'disabled'
      });
      return form.find('input, textarea').val('');
    };

    TemplateManager.prototype.enableForm = function() {
      var form;
      form = $('.template-form');
      return form.find('input, textarea').attr({
        disabled: null
      });
    };

    TemplateManager.prototype.clone = function() {
      var _this = this;
      delete this.selected.id;
      this.selected.name = 'Copy of ' + this.selected.name;
      return this.app.manager.save('templates', this.selected, function(err) {
        if (err) return _this.app.showError(err);
        _this.refresh();
        _this.edit(_this.selected);
        return _this.app.refreshTemplates();
      });
    };

    TemplateManager.prototype.remove = function() {
      var _this = this;
      return this.app.showPrompt('Are you sure want to remove template? It\'ll reset template of associated sheets', function() {
        return _this.app.manager.storage.select('sheets', ['template_id', _this.selected.id], function(err, data) {
          var config, sheet, _i, _len;
          if (err) return _this.app.showError(err);
          config = [];
          for (_i = 0, _len = data.length; _i < _len; _i++) {
            sheet = data[_i];
            delete sheet.template_id;
            config.push({
              type: 'update',
              stream: 'sheets',
              object: sheet
            });
          }
          config.push({
            type: 'removeCascade',
            stream: 'templates',
            object: _this.selected
          });
          return _this.app.manager.batch(config, function(err) {
            if (err) return _this.app.showError(err);
            _this.selected = null;
            _this.refresh();
            return _this.app.refreshTemplates();
          });
        });
      });
    };

    TemplateManager.prototype.save = function() {
      var config, name, _ref, _ref2,
        _this = this;
      name = this.editName.val().trim();
      if (!name) return this.app.showError('Name is empty');
      config = {};
      try {
        config = JSON.parse(this.editConfig.val());
      } catch (e) {
        return this.app.showError('Config is not JSON');
      }
      this.selected.name = name;
      this.selected.tag = this.editTag.val().trim();
      this.selected.width = (_ref = parseInt(this.editWidth.val())) != null ? _ref : this.selected.width;
      this.selected.height = (_ref2 = parseInt(this.editHeight.val())) != null ? _ref2 : this.selected.height;
      this.selected.type = this.editType.val().trim();
      this.selected.config = config;
      return this.app.manager.save('templates', this.selected, function(err) {
        if (err) return _this.app.showError(err);
        _this.refresh();
        _this.edit(_this.selected);
        return _this.app.refreshTemplates();
      });
    };

    TemplateManager.prototype.edit = function(template) {
      var _ref, _ref2, _ref3, _ref4, _ref5;
      if (template == null) {
        template = {
          width: 102,
          height: 144
        };
      }
      this.enableForm();
      log('edit', template);
      $('.template-save').attr({
        disabled: null
      });
      if (template.id) {
        $('.template-remove').attr({
          disabled: null
        });
        $('.template-clone').attr({
          disabled: null
        });
      } else {
        $('.template-remove').attr({
          disabled: 'disabled'
        });
        $('.template-clone').attr({
          disabled: 'disabled'
        });
      }
      this.selected = template;
      this.editName.val((_ref = template.name) != null ? _ref : 'Untitled').focus().select();
      this.editTag.val((_ref2 = template.tag) != null ? _ref2 : '');
      this.editWidth.val((_ref3 = template.width) != null ? _ref3 : 0);
      this.editHeight.val((_ref4 = template.height) != null ? _ref4 : 0);
      this.editType.val((_ref5 = template.type) != null ? _ref5 : '');
      return this.editConfig.val(template.config ? JSON.stringify(template.config, null, 2) : '{}');
    };

    TemplateManager.prototype.refresh = function(selectID) {
      var ul,
        _this = this;
      ul = $('.templates-list-ul');
      return this.app.manager.storage.select('templates', [], function(err, data) {
        var a, found, item, li, section, _fn, _i, _j, _len, _len2, _ref, _ref2;
        if (err) return _this.app.showError(err);
        ul.empty();
        found = false;
        data = _this.app.sortTemplates(data);
        for (_i = 0, _len = data.length; _i < _len; _i++) {
          section = data[_i];
          li = $(document.createElement('li')).addClass('nav-header');
          li.text(section.title);
          li.appendTo(ul);
          _ref = section.items;
          _fn = function(item, li) {
            return a.bind('click', function(e) {
              _this.edit(item);
              _this.refresh();
              return false;
            });
          };
          for (_j = 0, _len2 = _ref.length; _j < _len2; _j++) {
            item = _ref[_j];
            li = $(document.createElement('li'));
            if (((_ref2 = _this.selected) != null ? _ref2.id : void 0) === item.id) {
              found = true;
              li.addClass('active');
            }
            a = $(document.createElement('a')).attr({
              'href': '#'
            });
            a.text(item.name);
            a.appendTo(li);
            li.appendTo(ul);
            _fn(item, li);
          }
        }
        if (!found) return _this.disableForm();
      }, {
        order: ['tag', 'name']
      });
    };

    return TemplateManager;

  })();

  TemplateConfig = (function() {

    function TemplateConfig(app) {
      this.app = app;
    }

    TemplateConfig.prototype.configure = function(tmpl, sheet, controller) {};

    TemplateConfig.prototype.preciseCoord = function(value) {
      return Math.round(value * 10) / 10;
    };

    return TemplateConfig;

  })();

  SheetPlugin = (function() {

    function SheetPlugin(app, key) {
      this.app = app;
      this.key = key;
    }

    SheetPlugin.prototype.load = function(div, sheet, controller) {};

    SheetPlugin.prototype.getConfig = function(config) {
      var conf, _ref;
      conf = (_ref = config != null ? config["_" + this.key] : void 0) != null ? _ref : {};
      return conf;
    };

    SheetPlugin.prototype.setConfig = function(config, conf) {
      return config["_" + this.key] = conf;
    };

    SheetPlugin.prototype.addButton = function(div, cls, handler) {
      var a, i, parent,
        _this = this;
      parent = div.find('.sheet-toolbar-custom');
      a = $(document.createElement('a')).addClass('btn').appendTo(parent);
      a.attr({
        href: '#'
      });
      i = $(document.createElement('i')).addClass(cls).appendTo(a);
      return a.bind('click', function(e) {
        handler();
        return false;
      });
    };

    return SheetPlugin;

  })();

  window.TemplateConfig = TemplateConfig;

  window.SheetPlugin = SheetPlugin;

}).call(this);
