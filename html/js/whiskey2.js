(function() {
  var Notepad, Whiskey2,
    __slice = Array.prototype.slice;

  yepnope({
    load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/whiskey2.css', 'lib/lima1/net.js', 'lib/lima1/main.js'],
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
      var db, jqnet, storage,
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
        log('Sync started');
        return _this.syncAlert = _this.showAlert('Please stand by...', {
          persistent: true,
          content: _this.syncProgress
        });
      });
      this.manager.on_sync.on('finish', function() {
        log('Sync finished');
        return _this.syncAlert.remove();
      });
      this.oauth.token = this.manager.get('token', 'no-value');
      return this.manager.open(function(error) {
        if (error) {
          _this.manager = null;
          _this.showError(error);
        }
        _this.showAlert('Application started', {
          severity: 'success'
        });
        _this.oauth.on_token_error = function() {
          log('Asking for username/password');
          return _this.showLoginDialog();
        };
        _this.bindMain();
        return _this.refreshNotepads();
      });
    };

    Whiskey2.prototype.showLoginDialog = function() {
      $('#main-password').val('');
      return $('#main-login-dialog').modal('show');
    };

    Whiskey2.prototype.bindMain = function() {
      var _this = this;
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
        return $(this).tab('show');
      });
    };

    Whiskey2.prototype.dragSetType = function(e, type, value) {
      var _ref, _ref2;
      if (e != null ? (_ref = e.originalEvent) != null ? (_ref2 = _ref.dataTransfer) != null ? _ref2.setData : void 0 : void 0 : void 0) {
        return e.originalEvent.dataTransfer.setData(type, JSON.stringify(value));
      }
    };

    Whiskey2.prototype.dragGetType = function(e, type) {
      var _ref, _ref2, _ref3, _ref4;
      if (e != null ? (_ref = e.originalEvent) != null ? (_ref2 = _ref.dataTransfer) != null ? _ref2.getData : void 0 : void 0 : void 0) {
        try {
          return JSON.parse(e != null ? (_ref3 = e.originalEvent) != null ? (_ref4 = _ref3.dataTransfer) != null ? _ref4.getData(type) : void 0 : void 0 : void 0);
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

    Whiskey2.prototype.refreshNotepads = function(selectID) {
      var _this = this;
      $('#main-tabs li.main-tab-notepad').remove();
      $('#main-tabs-content .main-tab-notepad').remove();
      return this.manager.storage.select('notepads', [
        'archived', {
          op: '<>',
          "var": 1
        }
      ], function(err, arr) {
        var a, div, item, li, _fn, _i, _len, _results;
        if (err) return _this.showError(err);
        log('Notepads:', arr);
        _fn = function(a) {
          a.bind('dragover', function(e) {
            if (_this.dragHasType(e, 'custom/note')) return e.preventDefault();
          });
          a.bind('dragenter', function(e) {
            if (_this.dragHasType(e, 'custom/note')) {
              a.tab('show');
              return e.preventDefault();
            }
          });
          return a.bind('click', function(e) {
            e.preventDefault();
            return a.tab('show');
          });
        };
        _results = [];
        for (_i = 0, _len = arr.length; _i < _len; _i++) {
          item = arr[_i];
          log('Item', item);
          li = $(document.createElement('li')).addClass('main-tab-notepad');
          a = $(document.createElement('a')).attr({
            href: '#np' + item.id
          }).text(' ' + item.name).appendTo(li);
          a.prepend('<i class="icon-book"></i>');
          $('#main-tabs').prepend(li);
          div = $(document.createElement('div')).addClass('tab-pane main-tab-notepad').attr({
            id: 'np' + item.id
          });
          $('#main-tabs-content').append(div);
          _fn(a);
          _this.renderNotepad(div, item);
          if (item.id === selectID) {
            _results.push(a.tab('show'));
          } else {
            _results.push(void 0);
          }
        }
        return _results;
      });
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
      var div;
      div = $('#main-notepad-template').clone().appendTo(parent).removeClass('hide');
      return new Notepad(this, notepad, div);
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

    Whiskey2.prototype.sync = function() {
      var _this = this;
      if (!this.manager) return;
      return this.manager.sync(function(err) {
        if (err) return _this.showError(err);
      }, false, function(type) {
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
        log('Sync progress', w, type);
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
      width: 100,
      height: 141
    };

    Whiskey2.prototype.getTemplate = function(id) {
      return this.emptyTemplate;
    };

    Whiskey2.prototype.findByID = function(arr, id, start) {
      var i, _ref;
      if (start == null) start = 0;
      if (!id) return -1;
      for (i = start, _ref = arr.length; start <= _ref ? i < _ref : i > _ref; start <= _ref ? i++ : i--) {
        if (arr[i].id === id) return i;
      }
      return -1;
    };

    Whiskey2.prototype.removeWithNext = function(sink, array, items) {
      var index, item, _i, _len, _results;
      _results = [];
      for (_i = 0, _len = items.length; _i < _len; _i++) {
        item = items[_i];
        index = this.findByID(array, item.id);
        if (index === -1) continue;
        if (index > 0) {
          if (index < array.length - 1) {
            array[index - 1].next_id = array[index + 1].id;
          } else {
            array[index - 1].next_id = null;
          }
          if (this.findByID(sink, array[index - 1].id === -1)) {
            _results.push(sink.add(array[index - 1]));
          } else {
            _results.push(void 0);
          }
        } else {
          _results.push(void 0);
        }
      }
      return _results;
    };

    Whiskey2.prototype.addWithNext = function(sink, array, before, items) {
      var item, next_id, _i, _len;
      if (!(items != null ? items.length : void 0)) return;
      if (!(array != null ? array.length : void 0)) {
        next_id = null;
        for (_i = 0, _len = items.length; _i < _len; _i++) {
          item = items[_i];
          item.next_id = next_id;
          next_id = item.id;
        }
        return;
      }
      return next_id = null;
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
            items.splice(i, 1);
            items.splice(index, 0, found);
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

    return Whiskey2;

  })();

  Notepad = (function() {

    function Notepad(app, notepad, div) {
      var _this = this;
      this.app = app;
      this.notepad = notepad;
      this.div = div;
      this.div.find('.notepad-name').text(this.notepad.name);
      this.divMiniatures = div.find('.notepad-sheets');
      this.divContent = div.find('.notepad-content');
      this.div.find('.notepad-add-sheet').bind('click', function() {
        return _this.showSheetDialog({}, function() {
          return _this.reloadSheets();
        });
      });
      this.div.find('.notepad-reload-sheets').bind('click', function() {
        return _this.reloadSheets();
      });
      this.reloadSheets();
    }

    Notepad.prototype.showSheetDialog = function(sheet, handler) {
      var _ref,
        _this = this;
      $('#sheet-dialog-name').val((_ref = sheet.title) != null ? _ref : '');
      $('#sheet-dialog').modal('show');
      return $('#do-save-sheet-dialog').unbind('click').bind('click', function(e) {
        var name, _handler;
        name = $('#sheet-dialog-name').val().trim();
        if (!name) return _this.app.showError('Title is empty');
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
          return _this.app.manager.storage.create('sheets', sheet, _handler);
        } else {
          return _this.app.manager.storage.update('sheets', sheet, _handler);
        }
      });
    };

    Notepad.prototype.renderSheetMiniatures = function(sheets) {
      var div, divItem, divItemText, h, height, i, maxHeight, minHeight, oneItemHeight, sheet, step, _fn, _ref,
        _this = this;
      div = this.divMiniatures;
      div.empty();
      if (!(sheets != null ? sheets.length : void 0)) return;
      minHeight = 3;
      maxHeight = 30;
      oneItemHeight = 50;
      height = $(window).height() - 150;
      step = maxHeight;
      if (sheets.length > 1) {
        step = Math.floor((height - oneItemHeight) / (sheets.length - 1));
      }
      if (step > maxHeight) step = maxHeight;
      if (step < minHeight) step = minHeight;
      h = 0;
      _fn = function(i, sheet, divItem) {
        divItem.bind('click', function(e) {
          e.preventDefault();
          return _this.loadSheets(i);
        });
        divItem.bind('dragstart', function(e) {
          return _this.app.dragSetType(e, 'custom/sheet', sheet);
        });
        divItem.bind('dragover', function(e) {
          if (_this.app.dragHasType(e, 'custom/sheet')) e.preventDefault();
          return false;
        });
        divItem.bind('drop', function(e) {
          var index, otherSheet;
          otherSheet = _this.app.dragGetType(e, 'custom/sheet');
          index = _this.app.findByID(sheets, otherSheet != null ? otherSheet.id : void 0);
          log('Dropped sheet', otherSheet, index, i);
          if (otherSheet && index !== -1) e.stopPropagation();
          return false;
        });
        divItem.bind('mouseover', function(e) {
          return divItem.addClass('notepad-sheet-miniature-hover');
        });
        divItem.bind('dragenter', function(e) {
          divItem.addClass('notepad-sheet-miniature-hover');
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
        if (_this.lastSheetID) {
          index = _this.app.findByID(_this.sheets, _this.lastSheetID);
          if (index !== -1) return _this.loadSheets(index);
        }
      });
    };

    Notepad.prototype.maxSheetsVisible = 2;

    Notepad.prototype.loadSheets = function(index) {
      var count, div, totalWidth, width, _ref, _results;
      this.divContent.empty();
      width = this.divContent.innerWidth();
      totalWidth = 0;
      this.lastSheetID = (_ref = this.sheets[index]) != null ? _ref.id : void 0;
      count = 0;
      _results = [];
      while (totalWidth < width && count < this.maxSheetsVisible) {
        div = this.loadSheet(index);
        if (!div) break;
        totalWidth += div.outerWidth();
        index++;
        _results.push(count++);
      }
      return _results;
    };

    Notepad.prototype.showNotesDialog = function(sheet, note, handler) {
      var _ref,
        _this = this;
      $('#note-dialog').modal('show');
      $('#note-dialog-text').val((_ref = note.text) != null ? _ref : '').focus();
      $('#note-dialog-collapsed').attr({
        'checked': note.collapsed ? true : false
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
        log('Save', collapsed);
        if (!text) return _this.app.showError('Text is empty');
        note.text = text;
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
          note.sheet_id = sheet.id;
          return _this.app.manager.storage.create('notes', note, _handler);
        } else {
          return _this.app.manager.storage.update('notes', note, _handler);
        }
      });
    };

    Notepad.prototype.loadNotes = function(sheet, parent) {
      var loadNote, preciseEm,
        _this = this;
      preciseEm = function(value) {
        return Math.floor(value * 10 / _this.zoomFactor) / 10;
      };
      loadNote = function(note) {
        var div, i, line, lines, width, x, y, _ref, _ref2, _ref3, _ref4, _ref5, _ref6, _results;
        div = $(document.createElement('div')).addClass('note').appendTo(parent);
        div.attr({
          draggable: true
        });
        div.bind('dragstart', function(e) {
          var offset;
          offset = _this.app.dragGetOffset(e, div);
          return _this.app.dragSetType(e, 'custom/note', {
            id: note.id,
            x: offset.left,
            y: offset.top
          });
        });
        div.bind('dblclick', function(e) {
          _this.showNotesDialog(sheet, note, function() {
            return _this.reloadSheets();
          });
          e.preventDefault();
          return e.stopPropagation();
        });
        div.addClass('note-color0 note-color' + ((_ref = note.color) != null ? _ref : 0));
        width = (_ref2 = note.width) != null ? _ref2 : _this.noteWidths[0];
        width = preciseEm(width);
        x = preciseEm((_ref3 = note.x) != null ? _ref3 : 0);
        y = preciseEm((_ref4 = note.y) != null ? _ref4 : 0);
        div.css({
          width: "" + width + "em",
          left: "" + x + "em",
          top: "" + y + "em"
        });
        if (note.collapsed) div.addClass('note-collapsed');
        lines = ((_ref5 = note.text) != null ? _ref5 : '').split('\n');
        _results = [];
        for (i = 0, _ref6 = lines.length; 0 <= _ref6 ? i < _ref6 : i > _ref6; 0 <= _ref6 ? i++ : i--) {
          line = lines[i];
          if (!line) {
            if (i < lines.length - 1) {
              _results.push($(document.createElement('div')).addClass('note-br').html('&nbsp;').appendTo(div));
            } else {
              _results.push(void 0);
            }
          } else {
            _results.push($(document.createElement('div')).addClass('note-line').appendTo(div).text(line));
          }
        }
        return _results;
      };
      parent.empty();
      return this.app.manager.storage.select('notes', ['sheet_id', sheet.id], function(err, arr) {
        var item, _i, _len, _results;
        if (err) return _this.app.showError(err);
        _results = [];
        for (_i = 0, _len = arr.length; _i < _len; _i++) {
          item = arr[_i];
          _results.push(loadNote(item));
        }
        return _results;
      });
    };

    Notepad.prototype.noteWidths = [50, 75, 90, 125];

    Notepad.prototype.zoomFactor = 5;

    Notepad.prototype.colors = 5;

    Notepad.prototype.loadSheet = function(index) {
      var div, divContent, divTitle, divToolbar, height, offsetToCoordinates, sheet, template, width, _ref,
        _this = this;
      offsetToCoordinates = function(x, y) {
        return [Math.floor(x / divContent.width() * template.width), Math.floor(y / divContent.height() * template.height)];
      };
      sheet = this.sheets[index];
      if (!sheet) return null;
      template = this.app.getTemplate(sheet.template_id);
      div = $(document.createElement('div')).addClass('sheet').appendTo(this.divContent);
      divContent = $(document.createElement('div')).addClass('sheet_content').appendTo(div);
      width = Math.floor(template.width / this.zoomFactor);
      height = Math.floor(template.height / this.zoomFactor);
      divContent.css({
        width: "" + width + "em",
        height: "" + height + "em"
      });
      divTitle = $(document.createElement('div')).addClass('sheet_title').appendTo(div);
      divTitle.text((_ref = sheet.title) != null ? _ref : 'Untitled');
      divToolbar = $('#sheet-toolbar-template').clone().removeClass('hide').appendTo(div);
      divToolbar.find('.sheet-toolbar-edit').bind('click', function() {
        return _this.showSheetDialog(sheet, function() {
          return _this.reloadSheets();
        });
      });
      divContent.bind('dblclick', function(e) {
        var x, y, _ref2;
        _ref2 = offsetToCoordinates(e.offsetX, e.offsetY), x = _ref2[0], y = _ref2[1];
        _this.showNotesDialog(sheet, {
          x: x,
          y: y
        }, function() {
          return _this.reloadSheets();
        });
        return e.preventDefault();
      });
      divContent.bind('dragover', function(e) {
        if (_this.app.dragHasType(e, 'custom/note')) e.preventDefault();
        return false;
      });
      divContent.bind('drop', function(e) {
        var otherNote;
        if (_this.app.dragHasType(e, 'custom/note')) {
          otherNote = _this.app.dragGetType(e, 'custom/note');
          _this.app.manager.findOne('notes', otherNote != null ? otherNote.id : void 0, function(err, note) {
            var offset, x, y, _ref2;
            if (err) return _this.app.showError(err);
            offset = _this.app.dragGetOffset(e, divContent);
            _ref2 = offsetToCoordinates(offset.left - otherNote.x, offset.top - otherNote.y), x = _ref2[0], y = _ref2[1];
            note.x = x;
            note.y = y;
            note.sheet_id = sheet.id;
            return _this.app.manager.storage.update('notes', note, function(err) {
              if (err) return _this.app.showError(err);
              return _this.reloadSheets();
            });
          });
        }
        return false;
      });
      this.loadNotes(sheet, divContent);
      return div;
    };

    return Notepad;

  })();

}).call(this);
