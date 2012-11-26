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
        return _this.syncAlert = _this.showAlert('Please stand by...', {
          persistent: true,
          content: _this.syncProgress
        });
      });
      this.manager.on_sync.on('finish', function() {
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
      return this.manager.storage.select('notepads', [
        'archived', {
          op: '<>',
          "var": 1
        }
      ], function(err, arr) {
        var a, div, i, item, li, moveNotepad, _fn, _ref, _results;
        if (err) return _this.showError(err);
        $('#main-tabs li.main-tab-notepad').remove();
        $('#main-tabs-content .main-tab-notepad').remove();
        _this.notepads = {};
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
            if ((_this.dragHasType(e, 'custom/note')) || (_this.dragHasType(e, 'custom/sheet'))) {
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
            return a.tab('show');
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
        return _this.reloadSheets();
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

    Notepad.prototype.zoomInOut = function(direction) {
      if (direction == null) direction = 1;
      if (direction < 0 && this.zoom <= this.zoomStep) return;
      this.zoom += direction * this.zoomStep;
      return this.divContent.css({
        'font-size': "" + this.zoom + "em"
      });
    };

    Notepad.prototype.showSheetDialog = function(sheet, handler) {
      var _ref,
        _this = this;
      $('#sheet-dialog-name').val((_ref = sheet.title) != null ? _ref : '');
      $('#sheet-dialog').modal('show');
      $('#do-remove-sheet-dialog').unbind('click').bind('click', function(e) {
        if (!sheet.id) return;
        _this.app.showPrompt('Are you sure want to remove sheet? It will remove notes also', function() {
          return _this.removeSheets([sheet.id]);
        });
        return $('#sheet-dialog').modal('hide');
      });
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

    Notepad.prototype.renderSheetMiniatures = function(sheets) {
      var div, divItem, divItemText, h, height, i, maxHeight, minHeight, oneItemHeight, sheet, step, _fn, _ref,
        _this = this;
      div = this.divMiniatures;
      div.empty();
      this.selectedSheets = {};
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
          var sheetsData;
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

    Notepad.prototype.initSheetPlaceholders = function() {
      var div, divContent, divTitle, divToolbar, i, _ref, _results;
      this.sheetDivs = [];
      _results = [];
      for (i = 0, _ref = this.maxSheetsVisible; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        div = $(document.createElement('div')).addClass('sheet').appendTo(this.divContent);
        divContent = $(document.createElement('div')).addClass('sheet_content').appendTo(div);
        divTitle = $(document.createElement('div')).addClass('sheet_title').appendTo(div);
        divToolbar = $('#sheet-toolbar-template').clone().removeClass('hide').appendTo(div);
        _results.push(this.sheetDivs.push(div));
      }
      return _results;
    };

    Notepad.prototype.loadSheets = function(index) {
      var count, div, _ref, _results;
      this.selectedNotes = {};
      this.lastSheetID = (_ref = this.sheets[index]) != null ? _ref.id : void 0;
      count = 0;
      this.notes = [];
      _results = [];
      while (count < this.maxSheetsVisible) {
        div = this.sheetDivs[count];
        if (!this.sheets[index]) {
          div.hide();
        } else {
          div.show();
          this.loadSheet(index, div);
        }
        index++;
        _results.push(count++);
      }
      return _results;
    };

    Notepad.prototype.showNotesDialog = function(sheet, note, handler) {
      var a, adiv, color, colors, currentColor, currentWidth, width, widths, _i, _len, _ref, _ref2, _ref3, _ref4, _ref5, _ref6, _ref7,
        _this = this;
      $('#note-dialog').modal('show');
      $('#note-dialog-text').val((_ref = note.text) != null ? _ref : '').focus();
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

    Notepad.prototype.loadNotes = function(sheet, parent) {
      var loadNote,
        _this = this;
      loadNote = function(note) {
        var div, i, line, lines, width, x, y, _ref, _ref2, _ref3, _ref4, _ref5, _ref6, _results;
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
              y: offset.top
            });
          } else {
            return _this.app.dragSetType(e, 'custom/note', {
              id: note.id,
              x: offset.left,
              y: offset.top
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
        width = (_ref2 = note.width) != null ? _ref2 : _this.noteWidths[_this.noteDefaultWidth];
        width = _this.preciseEm(width);
        x = _this.preciseEm((_ref3 = note.x) != null ? _ref3 : 0);
        y = _this.preciseEm((_ref4 = note.y) != null ? _ref4 : 0);
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
      return this.app.manager.storage.select('notes', ['sheet_id', sheet.id], function(err, arr) {
        var item, _i, _len, _results;
        if (err) return _this.app.showError(err);
        parent.empty();
        _results = [];
        for (_i = 0, _len = arr.length; _i < _len; _i++) {
          item = arr[_i];
          loadNote(item);
          _results.push(_this.notes.push(item));
        }
        return _results;
      });
    };

    Notepad.prototype.noteWidths = [50, 75, 90, 125];

    Notepad.prototype.noteDefaultWidth = 1;

    Notepad.prototype.zoomFactor = 5;

    Notepad.prototype.colors = 8;

    Notepad.prototype.gridStep = 6;

    Notepad.prototype.stick = true;

    Notepad.prototype.loadSheet = function(index, div) {
      var clearSelector, divContent, divTitle, height, inRectangle, notesInRectangle, offsetToCoordinates, sheet, stickToGrid, template, width, _ref,
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
        return [Math.floor(x / divContent.width() * template.width), Math.floor(y / divContent.height() * template.height)];
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
      divContent = div.find('.sheet_content');
      width = Math.floor(template.width / this.zoomFactor);
      height = Math.floor(template.height / this.zoomFactor);
      divContent.css({
        width: "" + width + "em",
        height: "" + height + "em"
      });
      divTitle = div.find('.sheet_title');
      divTitle.text((_ref = sheet.title) != null ? _ref : 'Untitled');
      div.find('.sheet-toolbar-edit').unbind('click').bind('click', function() {
        return _this.showSheetDialog(sheet, function() {
          return _this.reloadSheets();
        });
      });
      divContent.unbind();
      divContent.bind('mousedown', function(e) {
        var coords, notes, offset, x, y, _ref2, _ref3;
        offset = _this.app.dragGetOffset(e, divContent);
        _ref2 = offsetToCoordinates(offset.left, offset.top), x = _ref2[0], y = _ref2[1];
        _ref3 = notesInRectangle(x, y, x, y), coords = _ref3[0], notes = _ref3[1];
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
        var coords, note, notes, offset, x, y, _i, _len, _ref2, _ref3;
        if (!e.ctrlKey && _this.selectorDiv) {
          clearSelector();
          return;
        }
        if (_this.selectorDiv && _this.selectorIndex === index) {
          offset = _this.app.dragGetOffset(e, divContent);
          _ref2 = offsetToCoordinates(offset.left, offset.top), x = _ref2[0], y = _ref2[1];
          _ref3 = notesInRectangle(x, y, _this.selectorX, _this.selectorY), coords = _ref3[0], notes = _ref3[1];
          for (_i = 0, _len = notes.length; _i < _len; _i++) {
            note = notes[_i];
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
        var coords, notes, offset, x, y, _ref2, _ref3;
        if (!e.ctrlKey && _this.selectorDiv) {
          clearSelector();
          return;
        }
        if (_this.selectorDiv && _this.selectorIndex === index) {
          offset = _this.app.dragGetOffset(e, divContent);
          _ref2 = offsetToCoordinates(offset.left, offset.top), x = _ref2[0], y = _ref2[1];
          _ref3 = notesInRectangle(x, y, _this.selectorX, _this.selectorY), coords = _ref3[0], notes = _ref3[1];
          return _this.selectorDiv.css({
            left: '' + _this.preciseEm(coords.x) + 'em',
            top: '' + _this.preciseEm(coords.y) + 'em',
            width: '' + _this.preciseEm(coords.width) + 'em',
            height: '' + _this.preciseEm(coords.height) + 'em'
          });
        }
      });
      divContent.bind('dblclick', function(e) {
        var x, y, _ref2, _ref3;
        _ref2 = offsetToCoordinates(e.offsetX, e.offsetY), x = _ref2[0], y = _ref2[1];
        _ref3 = stickToGrid(x, y), x = _ref3[0], y = _ref3[1];
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
        var config, id, offset, otherNote, x, y, _i, _len, _ref2, _ref3, _ref4;
        if (_this.app.dragHasType(e, 'custom/note')) {
          otherNote = _this.app.dragGetType(e, 'custom/note');
          offset = _this.app.dragGetOffset(e, divContent);
          _ref2 = offsetToCoordinates(offset.left - otherNote.x, offset.top - otherNote.y), x = _ref2[0], y = _ref2[1];
          _ref3 = stickToGrid(x, y), x = _ref3[0], y = _ref3[1];
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
            _ref4 = otherNote.ids;
            for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
              id = _ref4[_i];
              config.push({
                type: 'findOne',
                id: id,
                stream: 'notes'
              });
            }
            _this.app.manager.batch(config, function(err, arr) {
              var note, updates, _j, _len2;
              if (err) return _this.app.showError(err);
              updates = [];
              arr = arr.sort(function(a, b) {
                if (a.y < b.y) return -1;
                if (a.y > b.y) return 1;
                return a.x - b.x;
              });
              for (_j = 0, _len2 = arr.length; _j < _len2; _j++) {
                note = arr[_j];
                note.sheet_id = sheet.id;
                note.x = x;
                note.y = y;
                y += _this.gridStep * 2;
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
      this.loadNotes(sheet, divContent);
      return div;
    };

    return Notepad;

  })();

}).call(this);
