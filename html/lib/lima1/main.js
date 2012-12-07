(function() {
  var AirCacheProvider, AirDBProvider, CacheProvider, ChannelProvider, DBProvider, DataManager, DesktopChannelProvider, HTML5CacheProvider, HTML5Provider, PhoneGapCacheProvider, SchemaInfo, StorageProvider, data_table_template,
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

  DBProvider = (function() {

    function DBProvider(name, version) {
      this.name = name;
      this.version = version != null ? version : '1';
    }

    DBProvider.prototype.open = function(clean, handler) {
      if (clean == null) clean = true;
      return handler('open: Not implemented');
    };

    DBProvider.prototype.verify = function(schema, handler) {
      return handler('Not implemented');
    };

    DBProvider.prototype.query = function(line, params, handler) {
      return handler('Not implemented');
    };

    DBProvider.prototype.get = function(name, def) {
      return null;
    };

    DBProvider.prototype.is = function(name, def) {
      return def != null ? def : false;
    };

    DBProvider.prototype.set = function(name, value) {
      return null;
    };

    return DBProvider;

  })();

  ChannelProvider = (function() {

    ChannelProvider.prototype.type = 'none';

    function ChannelProvider(oauth) {
      this.oauth = oauth;
    }

    ChannelProvider.prototype.is_connected = function() {
      return false;
    };

    ChannelProvider.prototype.need_channel = function() {
      return false;
    };

    ChannelProvider.prototype.on_channel = function(channel) {};

    ChannelProvider.prototype.on_update = function(message) {};

    ChannelProvider.prototype.on_connected = function() {};

    ChannelProvider.prototype.on_disconnected = function() {};

    return ChannelProvider;

  })();

  DesktopChannelProvider = (function(_super) {

    __extends(DesktopChannelProvider, _super);

    DesktopChannelProvider.prototype.socket = null;

    DesktopChannelProvider.prototype.type = 'desktop';

    function DesktopChannelProvider(oauth) {
      this.oauth = oauth;
      ui.remoteScriptLoader(this.oauth.transport.uri + '/_ah/channel/jsapi', 'goog');
    }

    DesktopChannelProvider.prototype.is_connected = function() {
      return this.socket;
    };

    DesktopChannelProvider.prototype.need_channel = function() {
      log('Need channel:', this.socket, window._goog());
      if (!this.socket && window._goog() && window._goog().appengine) return true;
      return false;
    };

    DesktopChannelProvider.prototype.on_channel = function(channel) {
      var goog,
        _this = this;
      if (!this.need_channel()) return;
      goog = _goog();
      this.socket = new goog.appengine.Channel(channel);
      return this.socket.open({
        onopen: function() {
          log('Socket opened');
          return _this.on_connected();
        },
        onmessage: function(message) {
          log('Message from socket:', message, message.data);
          return _this.on_update(message);
        },
        onerror: function(err) {
          log('Socket error:', err);
          _this.socket = null;
          return _this.on_disconnected();
        },
        onclose: function() {
          log('Socket closed');
          _this.socket = null;
          return _this.on_disconnected();
        }
      });
    };

    return DesktopChannelProvider;

  })(ChannelProvider);

  CacheProvider = (function() {

    function CacheProvider(oauth, app, maxwidth) {
      this.oauth = oauth;
      this.app = app;
      this.maxwidth = maxwidth;
    }

    CacheProvider.prototype.store = function(name, path, handler) {
      return handler(null);
    };

    CacheProvider.prototype.path_to_name = function(path) {
      return path;
    };

    CacheProvider.prototype.get = function(name, handler) {
      return handler(null);
    };

    CacheProvider.prototype.upload = function(name, handler) {
      return handler(null);
    };

    CacheProvider.prototype.remove = function(name, handler) {
      return handler(null);
    };

    return CacheProvider;

  })();

  HTML5CacheProvider = (function(_super) {

    __extends(HTML5CacheProvider, _super);

    function HTML5CacheProvider(oauth, app, maxwidth) {
      var _this = this;
      this.oauth = oauth;
      this.app = app;
      this.maxwidth = maxwidth;
      this.fs = null;
      window.webkitStorageInfo.requestQuota(PERSISTENT, 100 * 1024 * 1024, function(bytes) {
        return window.webkitRequestFileSystem(window.PERSISTENT, bytes, function(fs) {
          _this.fs = fs;
          return fs.root.getDirectory('cache', {
            create: true
          }, function(dir) {
            _this.cacheDir = dir;
            return log('Filesystem ready', _this.fs, dir);
          }, function(err) {
            log('Error getting dir', err);
            return _this.fs = null;
          });
        }, function(err) {
          return log('Error requesting FS:', err);
        });
      }, function(err) {
        return log('Error requesting quota:', err);
      });
    }

    HTML5CacheProvider.prototype.path_to_name = function(path) {
      log('path_to_name', path);
      return path.name;
    };

    HTML5CacheProvider.prototype.store = function(name, path, handler) {
      var openForRead, openForWrite, readWrite,
        _this = this;
      if (!this.cacheDir) return handler('No filesystem');
      openForRead = function(file) {
        return openForWrite(name, file);
      };
      readWrite = function(data, file) {
        return file.createWriter(function(writer) {
          writer.onwriteend = function() {
            return handler(null, file.toURL());
          };
          writer.onerror = function(err) {
            log('Write failed', err);
            return handler('Write error');
          };
          return writer.write(data);
        }, function(err) {
          log('Write failed', err);
          return handler('Write error');
        });
      };
      openForWrite = function(name, data) {
        return _this.cacheDir.getFile(name, {
          create: true
        }, function(file) {
          return readWrite(data, file);
        }, function(err) {
          log('Create file failed', err);
          return handler('Write error');
        });
      };
      return openForRead(path);
    };

    HTML5CacheProvider.prototype.get = function(name, handler) {
      var downloadFile,
        _this = this;
      if (!this.cacheDir) return handler('No filesystem');
      downloadFile = function(name) {
        var url, xhr;
        log('File not found, downloading', name);
        url = "/rest/file/download?name=" + name + "&";
        if (_.endsWith(name, '.jpg')) url += "width=" + _this.maxwidth + "&";
        url = _this.oauth.getFullURL(_this.app, url);
        xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.responseType = 'blob';
        xhr.onload = function(e) {
          log('Load', xhr.response, e);
          if (xhr.response) {
            return _this.cacheDir.getFile(name, {
              create: true
            }, function(file) {
              return file.createWriter(function(writer) {
                writer.onwriteend = function() {
                  return handler(null, file.toURL());
                };
                writer.onerror = function(err) {
                  log('Write failed', err);
                  return handler('Write error');
                };
                return writer.write(xhr.response);
              }, function(err) {
                log('Write failed', err);
                return handler('Write error');
              });
            }, function(err) {
              log('Create file failed', err);
              return handler('Write error');
            });
          } else {
            return handler('HTTP error');
          }
        };
        xhr.onerror = function(e) {
          return log('XHR error', e, arguments);
        };
        return xhr.send();
      };
      return this.cacheDir.getFile(name, {
        create: false
      }, function(file) {
        log('File found:', file, file.toURL());
        return file.getMetadata(function(meta) {
          if (meta.size > 0) {
            return handler(null, file.toURL());
          } else {
            return downloadFile(name);
          }
        }, function(err) {
          log('Error getting metadata', err);
          return handler('File error');
        });
      }, function(err) {
        return downloadFile(name);
      });
    };

    HTML5CacheProvider.prototype.upload = function(name, handler) {
      var doUpload, getFileContents,
        _this = this;
      if (!this.cacheDir) return handler('No filesystem');
      getFileContents = function(name) {
        return _this.cacheDir.getFile(name, {
          create: false
        }, function(file) {
          log('File found:', name, file);
          return file.file(function(file) {
            return doUpload(file, "/rest/file/upload?name=" + name + "&");
          }, function(err) {
            log('Error reading', err);
            return handler('Read error');
          });
        }, function(err) {
          return handler(null, -2);
        });
      };
      doUpload = function(data, url) {
        var formData, xhr;
        xhr = new XMLHttpRequest();
        xhr.open('POST', _this.oauth.getFullURL(_this.app, url), true);
        formData = new FormData();
        formData.append('file', data);
        xhr.onload = function(e) {
          log('Upload done', e, xhr.status);
          if (xhr.status !== 200) {
            return handler('HTTP error');
          } else {
            return handler(null, -1);
          }
        };
        xhr.onerror = function(e) {
          log('XHR error', e, arguments);
          return handler('HTTP error');
        };
        return xhr.send(formData);
      };
      return getFileContents(name);
    };

    HTML5CacheProvider.prototype.remove = function(name, handler) {
      var _this = this;
      if (!this.cacheDir) return handler('No filesystem');
      return this.cacheDir.getFile(name, {
        create: false
      }, function(file) {
        log('File found:', file, file.toURL());
        return file.remove(function() {
          return handler(null);
        }, function(err) {
          return handler('File error');
        });
      }, function(err) {
        return handler(null);
      });
    };

    return HTML5CacheProvider;

  })(CacheProvider);

  PhoneGapCacheProvider = (function(_super) {

    __extends(PhoneGapCacheProvider, _super);

    function PhoneGapCacheProvider(oauth, app, maxwidth) {
      var _this = this;
      this.oauth = oauth;
      this.app = app;
      this.maxwidth = maxwidth;
      PhoneGap.addConstructor(function() {
        return PhoneGap.addPlugin('Cache', _this);
      });
    }

    PhoneGapCacheProvider.prototype.store = function(name, path, handler) {
      var _this = this;
      return PhoneGap.exec(function() {
        return handler(null);
      }, function(err) {
        return handler(err != null ? err : 'PhoneGap error');
      }, 'Cache', 'copy', [name, path]);
    };

    PhoneGapCacheProvider.prototype.get = function(name, handler) {
      var _this = this;
      return PhoneGap.exec(function(url) {
        return handler(null, url);
      }, function(err) {
        var url;
        url = "/rest/file/download?name=" + name + "&";
        if (_.endsWith(name, '.jpg')) url += "width=" + _this.maxwidth + "&";
        log('Download', url);
        return PhoneGap.exec(function(url) {
          return handler(null, url);
        }, function(err) {
          return handler(err != null ? err : 'PhoneGap error');
        }, 'Cache', 'download', [name, _this.oauth.getFullURL(_this.app, url)]);
      }, 'Cache', 'get', [name]);
    };

    PhoneGapCacheProvider.prototype.upload = function(name, handler) {
      var _this = this;
      return PhoneGap.exec(function() {
        return PhoneGap.exec(function() {
          return handler(null, -1);
        }, function(err) {
          return handler(err != null ? err : 'PhoneGap error');
        }, 'Cache', 'upload', [name, _this.oauth.getFullURL(_this.app, "/rest/file/upload?name=" + name + "&")]);
      }, function(err) {
        return handler(null, -2);
      }, 'Cache', 'get', [name]);
    };

    PhoneGapCacheProvider.prototype.remove = function(name, handler) {
      var _this = this;
      return PhoneGap.exec(function() {
        return handler(null);
      }, function(err) {
        return handler(err != null ? err : 'PhoneGap error');
      }, 'Cache', 'remove', [name]);
    };

    return PhoneGapCacheProvider;

  })(CacheProvider);

  AirCacheProvider = (function(_super) {

    __extends(AirCacheProvider, _super);

    function AirCacheProvider() {
      AirCacheProvider.__super__.constructor.apply(this, arguments);
    }

    AirCacheProvider.prototype._folder = function() {
      var folder;
      folder = air.File.applicationStorageDirectory.resolvePath('cache');
      if (!folder.exists) folder.createDirectory();
      return folder;
    };

    AirCacheProvider.prototype.path_to_name = function(path) {
      return path.nativePath;
    };

    AirCacheProvider.prototype.store = function(name, path, handler) {
      var file,
        _this = this;
      file = new air.File(path.nativePath);
      if (!file.exists) return handler('File not found');
      file.addEventListener('complete', function() {
        return handler(null);
      });
      file.addEventListener('ioError', function() {
        return handler('Error copying file');
      });
      return file.copyToAsync(this._folder().resolvePath(name), true);
    };

    AirCacheProvider.prototype.get = function(name, handler) {
      var file, loader, request, url,
        _this = this;
      file = this._folder().resolvePath(name);
      if (file.exists) return handler(null, file.url);
      loader = new air.URLLoader();
      url = "/rest/file/download?name=" + name + "&";
      if (_.endsWith(name, '.jpg')) url += "width=" + this.maxwidth + "&";
      log('Download', url);
      request = new air.URLRequest(this.oauth.getFullURL(this.app, url));
      loader.dataFormat = air.URLLoaderDataFormat.BINARY;
      loader.addEventListener('complete', function(e) {
        var stream;
        log('File arrived');
        stream = new air.FileStream();
        try {
          stream.open(file, air.FileMode.WRITE);
          stream.writeBytes(loader.data);
          stream.close();
          return handler(null, file.url);
        } catch (error) {
          return handler('Error writing data');
        }
      });
      loader.addEventListener('ioError', function() {
        log('File download error');
        return handler('Error downloading file');
      });
      return loader.load(request);
    };

    AirCacheProvider.prototype.upload = function(name, handler) {
      var file,
        _this = this;
      file = this._folder().resolvePath(name);
      if (!file.exists) return handler(null, -2);
      return this.oauth.rest(this.app, "/rest/file/upload?name=" + name + "&", null, function(err, data) {
        var request, vars;
        if (err) return handler('Error uploading file');
        log('Uploading:', _this.oauth.transport.uri, data.u);
        request = new air.URLRequest(data.u);
        request.method = air.URLRequestMethod.POST;
        request.contentType = 'multipart/form-data';
        vars = new air.URLVariables();
        file.addEventListener('ioError', function(e) {
          log('Upload error', e);
          return handler('Error uploading file');
        });
        file.addEventListener('uploadCompleteData', function() {
          log('File uploaded');
          return handler(null, -1);
        });
        return file.upload(request, 'file', false);
      });
    };

    AirCacheProvider.prototype.remove = function(name, handler) {
      var file;
      file = this._folder().resolvePath(name);
      try {
        if (file.exists) file.deleteFile();
        return handler(null);
      } catch (error) {
        return handler('Error removing file');
      }
    };

    return AirCacheProvider;

  })(CacheProvider);

  AirDBProvider = (function(_super) {

    __extends(AirDBProvider, _super);

    function AirDBProvider() {
      AirDBProvider.__super__.constructor.apply(this, arguments);
    }

    AirDBProvider.prototype.open = function(clean, handler, absolute) {
      var err, folder,
        _this = this;
      if (clean == null) clean = true;
      this.db = new air.SQLConnection();
      err = function(event) {
        log('open error', event);
        return handler(event.error.message);
      };
      this.db.addEventListener(air.SQLEvent.OPEN, function(event) {
        log('Open in open');
        _this.db.removeEventListener(air.SQLErrorEvent.ERROR, err);
        return handler(null);
      });
      this.db.addEventListener(air.SQLErrorEvent.ERROR, err);
      if (absolute) {
        folder = air.File.applicationDirectory;
      } else {
        folder = air.File.applicationStorageDirectory;
      }
      this.dbFile = folder.resolvePath(this.name);
      return this.db.openAsync(this.dbFile, 'create', null, false, 1024);
    };

    AirDBProvider.prototype.verify = function(schema, handler) {
      var do_reset_schema, err, on_schema,
        _this = this;
      log('Verify from here');
      err = function() {
        log('verify error', event);
        return do_reset_schema();
      };
      do_reset_schema = function() {
        var afterClose;
        log('Reset called');
        _this.db.removeEventListener(air.SQLErrorEvent.ERROR, err);
        afterClose = function() {
          if (_this.dbFile.exists) _this.dbFile.deleteFile();
          return _this.open(false, function(err) {
            var createStmt, sql, sqlsDone, _i, _len, _results;
            log('Open in verify:', err);
            if (err) return handler(err);
            sqlsDone = 0;
            _results = [];
            for (_i = 0, _len = schema.length; _i < _len; _i++) {
              sql = schema[_i];
              createStmt = new air.SQLStatement();
              createStmt.sqlConnection = _this.db;
              createStmt.addEventListener(air.SQLEvent.RESULT, function() {
                if (++sqlsDone >= schema.length) return handler(null);
              });
              createStmt.addEventListener(air.SQLErrorEvent.ERROR, function(event) {
                return handler(event.error.message);
              });
              createStmt.text = sql;
              _results.push(createStmt.execute());
            }
            return _results;
          });
        };
        _this.db.addEventListener('close', function(event) {
          log('Closed event');
          return setTimeout(function() {
            log('Should be closed');
            return afterClose();
          }, 1000);
        });
        _this.db.close();
        return log('DB closed');
      };
      on_schema = function(event) {
        var table, tables, _i, _len, _ref, _ref2;
        _this.db.removeEventListener(air.SQLEvent.SCHEMA, on_schema);
        tables = (_ref = (_ref2 = _this.db.getSchemaResult()) != null ? _ref2.tables : void 0) != null ? _ref : [];
        log('Got schema', tables, schema, _this.clean);
        _this.clean = false;
        _this.tables = [];
        for (_i = 0, _len = tables.length; _i < _len; _i++) {
          table = tables[_i];
          _this.tables.push(table.name);
        }
        if (_this.clean) {
          return do_reset_schema();
        } else {
          return handler(null);
        }
      };
      this.db.addEventListener(air.SQLEvent.SCHEMA, on_schema);
      this.db.addEventListener(air.SQLErrorEvent.ERROR, err);
      this.db.loadSchema(air.SQLTableSchema);
      return log('Requested schema');
    };

    AirDBProvider.prototype.query = function(line, params, handler) {
      var i, stmt, _ref,
        _this = this;
      stmt = new air.SQLStatement();
      stmt.sqlConnection = this.db;
      stmt.addEventListener(air.SQLEvent.RESULT, function(event) {
        var data, i, numResults, result, row;
        result = stmt.getResult();
        data = [];
        if (!result || !result.data) return handler(null, data);
        numResults = result.data.length;
        for (i = 0; 0 <= numResults ? i < numResults : i > numResults; 0 <= numResults ? i++ : i--) {
          row = result.data[i];
          data.push(row);
        }
        return handler(null, data);
      });
      stmt.addEventListener(air.SQLErrorEvent.ERROR, function(event) {
        return handler(event.error.message);
      });
      stmt.text = line;
      for (i = 0, _ref = params.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        stmt.parameters[i] = params[i];
      }
      return stmt.execute();
    };

    AirDBProvider.prototype.get = function(name, def) {
      var arr;
      arr = air.EncryptedLocalStore.getItem(name);
      if (!name) return def;
      try {
        return arr.readUTF();
      } catch (error) {

      }
      return def;
    };

    AirDBProvider.prototype.is = function(name, def) {
      var arr;
      arr = air.EncryptedLocalStore.getItem(name);
      if (!name) return def != null ? def : false;
      try {
        return arr.readUTF();
      } catch (error) {

      }
      return def === true || def === 'true' || def === 'yes';
    };

    AirDBProvider.prototype.set = function(name, value) {
      var arr;
      if (!name) return false;
      if (!value) air.EncryptedLocalStore.removeItem(name);
      arr = new air.ByteArray();
      arr.writeUTF('' + value);
      return air.EncryptedLocalStore.setItem(name, arr);
    };

    return AirDBProvider;

  })(DBProvider);

  HTML5Provider = (function(_super) {

    __extends(HTML5Provider, _super);

    function HTML5Provider() {
      HTML5Provider.__super__.constructor.apply(this, arguments);
    }

    HTML5Provider.prototype.open = function(clean, handler) {
      if (!(window && window.openDatabase)) {
        return handler('HTML5 DB not supported');
      }
      try {
        this.db = window.openDatabase(env.prefix + this.name, '', env.prefix + this.name, 1024 * 1024 * 10);
        log('Opened', this.db.version, this.version);
        this.version_match = true;
        this.clean = clean;
        return handler(null);
      } catch (error) {
        return handler(error.message);
      }
    };

    HTML5Provider.prototype._query = function(query, params, transaction, handler) {
      var _this = this;
      return transaction.executeSql(query, params, function(transaction, result) {
        var data, i, key, obj, value, _ref, _ref2;
        data = [];
        for (i = 0, _ref = result.rows.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
          obj = {};
          _ref2 = result.rows.item(i);
          for (key in _ref2) {
            if (!__hasProp.call(_ref2, key)) continue;
            value = _ref2[key];
            if (value) obj[key] = value;
          }
          data.push(obj);
        }
        return handler(null, data, transaction);
      }, function(transaction, error) {
        log('Error SQL', query, error);
        return handler(error.message);
      });
    };

    HTML5Provider.prototype.query = function(query, params, handler, transaction) {
      var _this = this;
      if (!this.db) {
        if (!this.db) handler("DB isn't opened");
        return;
      }
      if (transaction) {
        this._query(query, params, transaction, handler);
        return transaction;
      } else {
        this.db.transaction(function(transaction) {
          return _this._query(query, params, transaction, handler);
        }, function(error) {
          log('Error transaction', error);
          return handler(error.message);
        });
        return null;
      }
    };

    HTML5Provider.prototype.verify = function(schema, handler) {
      var _this = this;
      return this.query('select name, type from sqlite_master where type=? or type=? order by type desc', ['table', 'index'], function(err, res, tr) {
        var create_at, drop_at, row, _i, _len;
        log('SQL result', err, res, tr);
        if (err) return handler(err);
        _this.tables = [];
        for (_i = 0, _len = res.length; _i < _len; _i++) {
          row = res[_i];
          if (row.type === 'table' && !(_.startsWith(row.name, 'sqlite_') || _.startsWith(row.name, '_'))) {
            _this.tables.push(row.name);
          }
        }
        if (!_this.version_match || _this.clean || (_this.tables.length === 0)) {
          _this.clean = true;
          create_at = function(index) {
            if (index < schema.length) {
              log('Create SQL:', schema[index]);
              return _this.query(schema[index], [], function(err) {
                if (err) return handler(err);
                return create_at(index + 1);
              }, tr);
            } else {
              log('Changing version [', _this.db.version, ']=>[', _this.version, ']');
              if (!_this.version_match) {
                log('Do change *');
                return _this.db.changeVersion(_this.db.version || '', _this.version, function(tr) {
                  log('Transaction');
                  return handler(null, true);
                }, function(err) {
                  log('Version change error', err);
                  return handler(err);
                }, function() {
                  log('Changed version');
                  return handler(null, true);
                });
              } else {
                log('No change');
                return handler(null, false);
              }
            }
          };
          drop_at = function(index) {
            if (index < res.length) {
              if (res[index].name.substr(0, 2) === '__' || res[index].name.substr(0, 7) === 'sqlite_') {
                return drop_at(index + 1);
              }
              return _this.query('drop ' + res[index].type + ' if exists ' + res[index].name, [], function(err) {
                if (err) return handler(err);
                return drop_at(index + 1);
              }, tr);
            } else {
              return create_at(0);
            }
          };
          return drop_at(0);
        } else {
          return handler(null, false);
        }
      });
    };

    HTML5Provider.prototype.get = function(name, def) {
      var val;
      val = typeof window !== "undefined" && window !== null ? window.localStorage[env.prefix + name] : void 0;
      if (!val) val = def;
      return val;
    };

    HTML5Provider.prototype.is = function(name, def) {
      var val, _ref;
      if (def == null) def = false;
      val = (_ref = typeof window !== "undefined" && window !== null ? window.localStorage[env.prefix + name] : void 0) != null ? _ref : def;
      if (!val) return def != null ? def : false;
      return val === true || val === 'true' || val === 'yes';
    };

    HTML5Provider.prototype.set = function(name, value) {
      return typeof window !== "undefined" && window !== null ? window.localStorage[env.prefix + name] = value : void 0;
    };

    return HTML5Provider;

  })(DBProvider);

  SchemaInfo = (function() {

    SchemaInfo.prototype.tables = {};

    SchemaInfo.prototype.upgrades = 0;

    function SchemaInfo(json) {
      this.json = json;
      this.upgrades = this.parseSchema(this.json);
      this.rev = this.json._rev;
    }

    SchemaInfo.prototype.parseSchema = function(json) {
      var fk, fkTable, fkeys, item, name, pk, pkTable, upgrades, _i, _j, _len, _len2, _ref, _ref2, _ref3, _ref4;
      for (name in json) {
        item = json[name];
        if (_.startsWith(name, '_')) continue;
        this.tables[name] = this.parseTable(name, item);
      }
      fkeys = (_ref = json._fkeys) != null ? _ref : [];
      for (_i = 0, _len = fkeys.length; _i < _len; _i++) {
        item = fkeys[_i];
        pk = ((_ref2 = item.pk) != null ? _ref2 : '').split('.');
        fk = ((_ref3 = item.fk) != null ? _ref3 : '').split('.');
        pkTable = this.tables[pk[0]];
        fkTable = this.tables[fk[0]];
        if (pkTable && fkTable) {
          pkTable.fkeys.push({
            table: fk[0],
            field: fk[1]
          });
        }
      }
      upgrades = (_ref4 = json._upgrades) != null ? _ref4 : [];
      for (_j = 0, _len2 = upgrades.length; _j < _len2; _j++) {
        item = upgrades[_j];
        this.parseSchema(item);
      }
      return upgrades.length;
    };

    SchemaInfo.prototype.parseTable = function(name, item) {
      var arr, object, value, _i, _j, _k, _len, _len2, _len3, _ref, _ref2, _ref3;
      object = this.tables[name];
      if (!object) {
        object = {
          texts: {},
          numbers: {},
          indexes: [],
          fkeys: []
        };
      }
      arr = (_ref = item.texts) != null ? _ref : [];
      for (_i = 0, _len = arr.length; _i < _len; _i++) {
        value = arr[_i];
        object.texts[value] = true;
      }
      arr = (_ref2 = item.numbers) != null ? _ref2 : [];
      for (_j = 0, _len2 = arr.length; _j < _len2; _j++) {
        value = arr[_j];
        object.numbers[value] = true;
      }
      arr = (_ref3 = item.indexes) != null ? _ref3 : [];
      for (_k = 0, _len3 = arr.length; _k < _len3; _k++) {
        item = arr[_k];
        object.indexes.push(item);
      }
      return object;
    };

    SchemaInfo.prototype.upgradeSchema = function(from) {
      var name, otherTable, sqls, table, _ref;
      sqls = [];
      _ref = this.tables;
      for (name in _ref) {
        table = _ref[name];
        otherTable = from != null ? from.tables[name] : void 0;
        if (!otherTable) {
          this.createTable(name, table, sqls);
        } else {
          this.alterTable(name, table, otherTable, sqls);
        }
      }
      return sqls;
    };

    SchemaInfo.prototype.alterTable = function(name, table, other, sqls) {
      var field, i, _ref, _ref2, _results;
      for (field in table.texts) {
        if (!other.texts[field]) {
          sqls.push("alter table t_" + name + " add f_" + field + " text");
        }
      }
      for (field in table.numbers) {
        if (!other.numbers[field]) {
          sqls.push("alter table t_" + name + " add f_" + field + " integer");
        }
      }
      _results = [];
      for (i = _ref = other.indexes.length, _ref2 = table.indexes.length; _ref <= _ref2 ? i < _ref2 : i > _ref2; _ref <= _ref2 ? i++ : i--) {
        _results.push(this.createIndex(name, i, table.indexes[i], sqls));
      }
      return _results;
    };

    SchemaInfo.prototype.createTable = function(name, table, sqls) {
      var field, i, sql, _ref, _results;
      sql = "create table if not exists t_" + name + " " + data_table_template;
      for (field in table.texts) {
        sql += ", f_" + field + " text";
      }
      for (field in table.numbers) {
        sql += ", f_" + field + " integer";
      }
      sqls.push(sql + ')');
      _results = [];
      for (i = 0, _ref = table.indexes.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        _results.push(this.createIndex(name, i, table.indexes[i], sqls));
      }
      return _results;
    };

    SchemaInfo.prototype.createIndex = function(name, i, index, sqls) {
      var field, sql, _i, _len;
      sql = "create index if not exists i_" + name + "_" + i + " on t_" + name + " (status";
      for (_i = 0, _len = index.length; _i < _len; _i++) {
        field = index[_i];
        sql += ", f_" + field;
      }
      return sqls.push(sql + ')');
    };

    return SchemaInfo;

  })();

  data_table_template = '(id integer primary key, status integer default 0, updated integer default 0, own integer default 1, stream text, data text';

  StorageProvider = (function() {

    StorageProvider.prototype.last_id = 0;

    StorageProvider.prototype.db_schema = ['create table if not exists updates (id integer primary key, version_in integer, version_out integer, version text)', 'create table if not exists schema (id integer primary key, token text, schema text)', 'create table if not exists uploads (id integer primary key, path text, name text, status integer)'];

    StorageProvider.prototype.SYNC_NETWORK = 0;

    StorageProvider.prototype.SYNC_READ_DATA = 1;

    StorageProvider.prototype.SYNC_WRITE_DATA = 2;

    function StorageProvider(db) {
      this.db = db;
      this.on_channel_state = new EventEmitter(this);
      this.stateListener = new EventEmitter(this);
    }

    StorageProvider.prototype.open = function(handler) {
      var _this = this;
      return this.db.open(false, function(err) {
        log('StorageProvider::Open result:', err);
        if (!err) {
          return _this.db.verify(_this.db_schema, function(err, reset) {
            log('StorageProvider::Verify result', err, reset);
            if (err) return handler(err);
            return _this.db.query('select schema, token from schema', [], function(err, data) {
              var schema;
              if (err) return handler(err);
              if (data.length > 0) {
                schema = JSON.parse(data[0].schema);
                _this.token = data[0].token;
                _this.schemaInfo = new SchemaInfo(schema);
                log('SchemaInfo', _this.schemaInfo);
              }
              return handler(null);
            });
          });
        }
      });
    };

    StorageProvider.prototype.get = function(name, def) {
      return this.db.get(name, def);
    };

    StorageProvider.prototype.set = function(name, value) {
      return this.db.set(name, value);
    };

    StorageProvider.prototype._precheck = function(stream, handler) {
      if (!this.schemaInfo) {
        handler('Not synchronized');
        return false;
      }
      if (!this.schemaInfo.tables[stream]) {
        handler('Unsupported stream');
        return false;
      }
      return true;
    };

    StorageProvider.prototype.set_token = function(token, handler) {
      var _this = this;
      this.token = token;
      return this.db.query('update schema set token=?', [token], function(err) {
        if (handler) return handler(err);
      });
    };

    StorageProvider.prototype.sync = function(app, oauth, handler, progress_handler) {
      var clean_sync, finish_sync, get_last_sync, in_from, in_items, out_from, out_items, receive_out, reset_schema, schema_uri, send_in, upload_file,
        _this = this;
      oauth.token = this.token;
      reset_schema = false;
      clean_sync = false;
      in_from = 0;
      out_from = 0;
      out_items = 0;
      in_items = 0;
      finish_sync = function(err) {
        if (err) return handler(err);
        _this.has_update = false;
        if (_this.channel && _this.channel.is_connected()) {
          _this.on_channel_state.emit('state', {
            state: _this.CHANNEL_NO_DATA
          });
        }
        progress_handler(_this.SYNC_WRITE_DATA);
        return _this.db.query('insert into updates (id, version_in, version_out) values (?, ?, ?)', [_this._id(), in_from, out_from], function() {
          var item, name, _ref;
          _ref = _this.schemaInfo.tables;
          for (name in _ref) {
            item = _ref[name];
            _this.db.query('delete from t_' + name + ' where status=? and updated<=?', [3, in_from], function() {});
          }
          return handler(err, {
            "in": in_items,
            out: out_items
          });
        });
      };
      upload_file = function() {
        var _ref;
        if (_.indexOf((_ref = _this.db) != null ? _ref.tables : void 0, 'uploads') === -1 || !_this.cache) {
          return send_in(null);
        }
        progress_handler(_this.SYNC_READ_DATA);
        return _this.db.query('select id, name, status from uploads order by id limit 1', [], function(err, data) {
          var remove_entry, row;
          if (err) return finish_sync(err);
          if (data.length === 0) return send_in(null);
          row = data[0];
          remove_entry = function() {
            progress_handler(_this.SYNC_WRITE_DATA);
            _this.cache.remove(row.name, function() {});
            return _this.db.query('delete from uploads where id=?', [row.id], function(err, res) {
              if (err) return finish_sync(err);
              return upload_file(null);
            });
          };
          progress_handler(_this.SYNC_NETWORK);
          if (row.status === 3) {
            return oauth.rest(app, '/rest/file/remove?name=' + row.name + '&', null, function(err, res) {
              if (err) return finish_sync(err);
              return remove_entry(null);
            });
          } else {
            return _this.cache.upload(row.name, function(err) {
              if (err) return finish_sync(err);
              return remove_entry(null);
            });
          }
        });
      };
      receive_out = function(transaction) {
        var url;
        url = "/rest/out?from=" + out_from + "&";
        if (!clean_sync) url += "inc=yes&";
        progress_handler(_this.SYNC_NETWORK);
        return oauth.rest(app, url, null, function(err, res) {
          var arr, i, item, last, object, _results;
          if (err) return finish_sync(err);
          progress_handler(_this.SYNC_WRITE_DATA);
          arr = res.a;
          if (arr.length === 0) {
            out_from = res.u;
            return finish_sync(null);
          } else {
            _results = [];
            for (i in arr) {
              item = arr[i];
              last = parseInt(i) === arr.length - 1;
              object = null;
              out_from = item.u;
              in_items++;
              try {
                object = JSON.parse(item.o);
              } catch (e) {
                log('Error parsing object', e);
              }
              _results.push((function(last) {
                var tr;
                return tr = _this.create(item.s, object, function(err, _data, tr) {
                  if (last) return receive_out(null);
                }, {
                  status: item.st,
                  updated: item.u,
                  own: 0,
                  internal: true
                });
              })(last));
            }
            return _results;
          }
        });
      };
      send_in = function() {
        var name, slots, sql, vars;
        progress_handler(_this.SYNC_READ_DATA);
        slots = 10;
        sql = [];
        vars = [];
        for (name in _this.schemaInfo.tables) {
          sql.push('select id, stream, data, updated, status from t_' + name + ' where own=? and updated>?');
          vars.push(1);
          vars.push(in_from);
        }
        if (sql.length === 0) return receive_out(null);
        return _this.db.query(sql.join(' union ') + ' order by updated', vars, function(err, data, tr) {
          var i, item, result, slots_needed, slots_used;
          if (err) return finish_sync(err);
          result = [];
          slots_used = 0;
          for (i in data) {
            item = data[i];
            slots_needed = 1;
            if (slots_needed + slots_used > slots) break;
            slots_used += slots_needed;
            result.push({
              s: item.stream,
              st: item.status,
              u: item.updated,
              o: item.data,
              i: item.id
            });
            in_from = item.updated;
            out_items++;
          }
          if (result.length === 0) {
            receive_out(null);
            return;
          }
          progress_handler(_this.SYNC_NETWORK);
          return oauth.rest(app, '/rest/in?', JSON.stringify({
            a: result
          }), function(err, res) {
            if (err) return finish_sync(err);
            return send_in(null);
          });
        });
      };
      get_last_sync = function() {
        progress_handler(_this.SYNC_READ_DATA);
        return _this.db.query('select * from updates order by id desc', [], function(err, data) {
          if (err) return finish_sync(err);
          if (data.length > 0) {
            in_from = data[0].version_in || 0;
            out_from = data[0].version_out || 0;
            if (!clean_sync && out_from > 0) clean_sync = false;
          }
          return upload_file(null);
        });
      };
      schema_uri = '/rest/schema?';
      if (this.channel && this.channel.need_channel()) {
        schema_uri += 'channel=get&type=' + this.channel.type + '&';
      }
      progress_handler(this.SYNC_NETWORK);
      return oauth.rest(app, schema_uri, null, function(err, schema) {
        var gr, schemaInfo, sql, sqls, _i, _len, _results;
        log('After schema', err, schema);
        if (err) return finish_sync(err);
        if (_this.channel && schema._channel) {
          _this.channel.on_channel(schema._channel);
        }
        if (!_this.schemaInfo || _this.schemaInfo.rev < schema._rev) {
          schemaInfo = new SchemaInfo(schema);
          sqls = schemaInfo.upgradeSchema(_this.schemaInfo);
          gr = new AsyncGrouper(sqls.length, function(gr) {
            err = gr.findError();
            if (err) return finish_sync(err);
            _this.schemaInfo = schemaInfo;
            gr = new AsyncGrouper(2, function(gr) {
              err = gr.findError();
              if (err) return finish_sync(err);
              return get_last_sync(null);
            });
            _this.db.query('delete from schema', [], gr.fn);
            return _this.db.query('insert into schema (id, token, schema) values (?, ?, ?)', [_this._id(), _this.token, JSON.stringify(schema)], gr.fn);
          });
          _results = [];
          for (_i = 0, _len = sqls.length; _i < _len; _i++) {
            sql = sqls[_i];
            log('Upgrade schema:', sql);
            _results.push(_this.db.query(sql, [], gr.fn));
          }
          return _results;
        } else {
          return get_last_sync(null);
        }
      }, {
        check: true
      });
    };

    StorageProvider.prototype._id = function(id) {
      if (!id) id = new Date().getTime();
      while (id <= this.last_id) {
        id++;
      }
      this.last_id = id;
      return id;
    };

    StorageProvider.prototype.CHANNEL_DATA = 1;

    StorageProvider.prototype.CHANNEL_NO_DATA = 2;

    StorageProvider.prototype.CHANNEL_NO_CONNECTION = 3;

    StorageProvider.prototype.set_channel_provider = function(channel) {
      var _this = this;
      this.channel = channel;
      this.channel.on_update = function(message) {
        _this.has_update = true;
        return _this.on_channel_state.emit('state', {
          state: _this.CHANNEL_DATA
        });
      };
      this.channel.on_connected = function() {
        if (_this.has_update) {
          return _this.on_channel_state.emit('state', {
            state: _this.CHANNEL_DATA
          });
        } else {
          return _this.on_channel_state.emit('state', {
            state: _this.CHANNEL_NO_DATA
          });
        }
      };
      return this.channel.on_disconnected = function() {
        return _this.on_channel_state.emit('state', {
          state: _this.CHANNEL_NO_CONNECTION
        });
      };
    };

    StorageProvider.prototype.on_change = function(type, stream, id) {};

    StorageProvider.prototype.uploadFile = function(path, handler) {
      var dotloc, ext, fileName, name,
        _this = this;
      if (!this.cache) return handler('Not supported');
      fileName = this.cache.path_to_name(path);
      dotloc = fileName.lastIndexOf('.');
      ext = '.bin';
      if (dotloc !== -1) ext = fileName.substr(dotloc);
      name = '' + this._id() + ext.toLowerCase();
      return this.cache.store(name, path, function(err) {
        if (err) return handler(err);
        return _this.db.query('insert into uploads (id, path, name, status) values (?, ?, ?, ?)', [_this._id(), fileName, name, 1], function(err) {
          if (err) return handler(err);
          return handler(null, name);
        });
      });
    };

    StorageProvider.prototype.getFile = function(name, handler) {
      var _this = this;
      if (!this.cache) return handler('Not supported');
      this.cache.oauth.token = this.token;
      return this.cache.get(name, function(err, uri) {
        if (err) return handler(err);
        return handler(null, uri);
      });
    };

    StorageProvider.prototype.removeFile = function(name, handler) {
      var _this = this;
      if (!this.cache) return handler('Not supported');
      return this.cache.remove(name, function() {
        return _this.db.query('select id from uploads where name=? and status=?', [name, 1], function(err, data) {
          var query, vars;
          if (err) return handler(err);
          query = null;
          vars = null;
          if (data.length > 0) {
            query = 'delete from uploads where name=?';
            vars = [name];
          } else {
            query = 'insert into uploads (id, path, name, status) values (?, ?, ?, ?)';
            vars = [_this._id(), null, name, 3];
          }
          return _this.db.query(query, vars, function(err) {
            return handler(err);
          });
        });
      });
    };

    StorageProvider.prototype.create = function(stream, object, handler, options) {
      var field, fields, numbers, questions, texts, values, _ref, _ref2, _ref3, _ref4, _ref5, _ref6, _ref7,
        _this = this;
      if (!this._precheck(stream, handler)) return;
      if (!object.id) object.id = this._id();
      questions = '?, ?, ?, ?, ?, ?';
      fields = 'id, status, updated, own, stream, data';
      values = [object.id, (_ref = options != null ? options.status : void 0) != null ? _ref : 1, (_ref2 = options != null ? options.updated : void 0) != null ? _ref2 : object.id, (_ref3 = options != null ? options.own : void 0) != null ? _ref3 : 1, stream, JSON.stringify(object)];
      numbers = (_ref4 = this.schemaInfo.tables[stream].numbers) != null ? _ref4 : {};
      texts = (_ref5 = this.schemaInfo.tables[stream].texts) != null ? _ref5 : {};
      for (field in numbers) {
        questions += ', ?';
        fields += ', f_' + field;
        values.push((_ref6 = object[field]) != null ? _ref6 : null);
      }
      for (field in texts) {
        questions += ', ?';
        fields += ', f_' + field;
        values.push((_ref7 = object[field]) != null ? _ref7 : null);
      }
      return this.db.query('insert or replace into t_' + stream + ' (' + fields + ') values (' + questions + ')', values, function(err, _data, transaction) {
        if (err) {
          return handler(err);
        } else {
          if (!(options != null ? options.internal : void 0)) {
            _this.on_change('create', stream, object.id);
          }
          _this.stateListener.emit('change');
          return handler(null, object, transaction);
        }
      }, options != null ? options.transaction : void 0);
    };

    StorageProvider.prototype.update = function(stream, object, handler) {
      var field, fields, numbers, texts, values, _ref, _ref2, _ref3, _ref4,
        _this = this;
      if (!this._precheck(stream, handler)) return;
      if (!object || !object.id) return handler('Invalid object ID');
      fields = 'status=?, updated=?, own=?, data=?';
      values = [2, this._id(), 1, JSON.stringify(object)];
      numbers = (_ref = this.schemaInfo.tables[stream].numbers) != null ? _ref : {};
      texts = (_ref2 = this.schemaInfo.tables[stream].texts) != null ? _ref2 : {};
      for (field in numbers) {
        fields += ', f_' + field + '=?';
        values.push((_ref3 = object[field]) != null ? _ref3 : null);
      }
      for (field in texts) {
        fields += ', f_' + field + '=?';
        values.push((_ref4 = object[field]) != null ? _ref4 : null);
      }
      values.push(object.id);
      values.push(stream);
      return this.db.query('update t_' + stream + ' set ' + fields + ' where id=? and stream=?', values, function(err) {
        if (!err) {
          _this.on_change('update', stream, object.id);
          _this.stateListener.emit('change');
        }
        return handler(err);
      });
    };

    StorageProvider.prototype.remove = function(stream, object, handler) {
      var _this = this;
      if (!this._precheck(stream, handler)) return;
      if (!object || !object.id) return handler('Invalid object ID');
      return this.db.query('update t_' + stream + ' set status=?, updated=?, own=? where  id=? and stream=?', [3, this._id(new Date().getTime()), 1, object.id, stream], function(err) {
        if (!err) {
          _this.on_change('remove', stream, object.id);
          _this.stateListener.emit('change');
        }
        return handler(err);
      });
    };

    StorageProvider.prototype.select = function(stream, query, handler, options) {
      var ar, arr, array_to_query, asc, extract_fields, fields, group_by, limit, need_id, order, sql, values, where, _i, _j, _len, _len2,
        _this = this;
      if (!this._precheck(stream, handler)) return;
      extract_fields = function(stream) {
        var fields, name, _ref, _ref2;
        fields = {
          id: 'id'
        };
        for (name in (_ref = _this.schemaInfo.tables[stream].texts) != null ? _ref : {}) {
          fields[name] = 'f_' + name;
        }
        for (name in (_ref2 = _this.schemaInfo.tables[stream].numbers) != null ? _ref2 : {}) {
          fields[name] = 'f_' + name;
        }
        return fields;
      };
      fields = extract_fields(stream);
      values = [3];
      array_to_query = function(fields, arr, op) {
        var f, i, name, res, result, value, wherePart, _ref, _ref2, _ref3, _ref4, _ref5;
        if (arr == null) arr = [];
        if (op == null) op = 'and';
        result = [];
        for (i = 0, _ref = arr.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
          name = arr[i];
          if (name != null ? name.op : void 0) {
            if (name.op === 'not') {
              res = array_to_query(fields, (_ref2 = name["var"]) != null ? _ref2 : []);
              if (res) result.push('not (' + res + ')');
            } else {
              res = array_to_query(fields, (_ref3 = name["var"]) != null ? _ref3 : [], name.op);
              if (res) result.push(res);
            }
          } else {
            if (fields[name]) {
              value = arr[i + 1];
              if (value != null ? value.op : void 0) {
                if (value.op === 'in') {
                  f = extract_fields(value.stream);
                  values.push(3);
                  wherePart = array_to_query(f, (_ref4 = value.query) != null ? _ref4 : []);
                  result.push('' + fields[name] + ' in (select ' + f[value.field] + ' from t_' + value.stream + ' where status<>?' + (wherePart ? ' and ' + wherePart : '') + ')');
                } else {
                  if (value["var"]) {
                    result.push(fields[name] + ' ' + value.op + ' ?');
                    values.push((_ref5 = value["var"]) != null ? _ref5 : null);
                  } else {
                    result.push(fields[name] + ' ' + value.op);
                  }
                }
              } else {
                result.push(fields[name] + '=?');
                values.push(value != null ? value : null);
              }
            }
            i++;
          }
        }
        if (result.length > 0) {
          return '(' + (result.join(") " + op + " (")) + ')';
        } else {
          return null;
        }
      };
      where = array_to_query(fields, query != null ? query : []);
      group_by = [];
      if (options != null ? options.group : void 0) {
        arr = options != null ? options.group : void 0;
        if (!$.isArray(arr)) arr = [arr];
        for (_i = 0, _len = arr.length; _i < _len; _i++) {
          ar = arr[_i];
          if (fields[ar] || 'id' === ar) group_by.push(fields[ar]);
        }
      }
      order = [];
      need_id = true;
      if (options != null ? options.order : void 0) {
        arr = options != null ? options.order : void 0;
        if (!$.isArray(arr)) arr = [arr];
        for (_j = 0, _len2 = arr.length; _j < _len2; _j++) {
          ar = arr[_j];
          asc = 'asc';
          if ((ar != null ? ar.charAt : void 0) && (ar != null ? ar.charAt(0) : void 0) === '!') {
            ar = ar.substr(1);
            asc = 'desc';
          }
          if (fields[ar] || 'id' === ar) {
            order.push(fields[ar] + ' ' + asc);
            if (ar === 'id') need_id = false;
          }
        }
      }
      if (options != null ? options.group : void 0) need_id = false;
      if (need_id) order.push('id asc');
      limit = '';
      if (options != null ? options.limit : void 0) {
        limit = ' limit ' + (options != null ? options.limit : void 0);
      }
      sql = 'select ';
      if (options != null ? options.field : void 0) {
        if (options != null ? options.distinct : void 0) sql += 'distinct ';
        sql += 'f_' + (options != null ? options.field : void 0);
      } else {
        sql += 'data';
      }
      return this.db.query(sql + ' from t_' + stream + ' where status<>? ' + (where ? 'and ' + where : '') + (group_by.length > 0 ? ' group by ' + group_by.join(',') : '') + ' order by ' + (order.join(',')) + limit, values, function(err, data) {
        var item, itm, result, _k, _len3;
        if (err) return handler(err);
        result = [];
        for (_k = 0, _len3 = data.length; _k < _len3; _k++) {
          item = data[_k];
          if (options != null ? options.field : void 0) {
            itm = {};
            itm[options != null ? options.field : void 0] = item['f_' + (options != null ? options.field : void 0)];
            result.push(itm);
          } else {
            try {
              result.push(JSON.parse(item.data));
            } catch (err) {

            }
          }
        }
        return handler(null, result);
      });
    };

    return StorageProvider;

  })();

  DataManager = (function() {

    function DataManager(app, oauth, storage) {
      var _this = this;
      this.app = app;
      this.oauth = oauth;
      this.storage = storage;
      this.on_sync = new EventEmitter(this);
      this.oauth.on_new_token = function(token) {
        return _this.storage.set_token(token);
      };
      this.storage.on_channel_state.on('state', function(evt) {
        return _this.on_channel_state(evt.state);
      });
    }

    DataManager.prototype.sync_timeout = 30;

    DataManager.prototype.channel_timeout = 60 * 15;

    DataManager.prototype.timeout_id = null;

    DataManager.prototype.in_sync = false;

    DataManager.prototype.open = function(handler) {
      var _this = this;
      return this.storage.open(function(err) {
        log('Open result', err);
        if (err) return handler(err);
        _this.storage.stateListener.on('change', function() {
          return _this.schedule_sync();
        });
        return handler(null);
      });
    };

    DataManager.prototype.unschedule_sync = function() {
      if (this.timeout_id) {
        clearTimeout(this.timeout_id);
        return this.timeout_id = null;
      }
    };

    DataManager.prototype.schedule_sync = function() {
      var _this = this;
      this.unschedule_sync(null);
      return this.timeout_id = setTimeout(function() {
        return _this.on_scheduled_sync(null);
      }, 1000 * this.sync_timeout);
    };

    DataManager.prototype.on_scheduled_sync = function() {};

    DataManager.prototype.on_channel_state = function(state) {
      var _this = this;
      if (state === this.storage.CHANNEL_NO_CONNECTION) this.schedule_sync();
      if (!this.timeout_id && state === this.storage.CHANNEL_DATA) {
        log('Scheduling sync because of channel');
        return this.timeout_id = setTimeout(function() {
          return _this.on_scheduled_sync(null);
        }, 1000 * this.channel_timeout);
      }
    };

    DataManager.prototype.get_backup_url = function(type, from) {
      var dt, fname, url;
      dt = new Date();
      fname = ("" + this.app + "-" + type + "-") + dt.format('yymmdd-HHMM') + '.zip';
      url = "/rest/backup?fname=" + fname + "&type=" + type + "&";
      if (from) url += 'from=' + from + '&';
      return this.oauth.getFullURL(this.app, url);
    };

    DataManager.prototype.findOne = function(stream, id, handler) {
      var _this = this;
      return this.storage.select(stream, ['id', id], function(err, data) {
        if (err) return handler(err);
        if (data.length === 0) return handler('Not found');
        return handler(null, data[0]);
      });
    };

    DataManager.prototype.removeCascade = function(stream, object, handler) {
      var gr, key, keys, select, selects, table, _i, _j, _len, _len2, _results,
        _this = this;
      if (!this.storage._precheck(stream, handler)) return;
      table = this.storage.schemaInfo.tables[stream];
      keys = table.fkeys;
      selects = [];
      for (_i = 0, _len = keys.length; _i < _len; _i++) {
        key = keys[_i];
        selects.push({
          stream: key.table,
          query: [key.field, object.id]
        });
      }
      gr = new AsyncGrouper(selects.length, function(gr) {
        var arr, config, data, err, i, _j, _len2, _ref, _ref2;
        err = gr.findError();
        if (err) return handler(err);
        config = [];
        config.push({
          type: 'remove',
          stream: stream,
          object: object
        });
        for (i = 0, _ref = gr.results.length; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
          arr = gr.results[i];
          _ref2 = arr[0];
          for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
            data = _ref2[_j];
            config.push({
              type: 'removeCascade',
              stream: selects[i].stream,
              object: data
            });
          }
        }
        return _this.batch(config, handler);
      });
      _results = [];
      for (_j = 0, _len2 = selects.length; _j < _len2; _j++) {
        select = selects[_j];
        _results.push(this.storage.select(select.stream, select.query, gr.fn));
      }
      return _results;
    };

    DataManager.prototype.batch = function(config, handler) {
      var ag, item, _i, _len, _results,
        _this = this;
      if (config == null) config = [];
      ag = new AsyncGrouper(config.length, function(ag) {
        var arr, err, result, _i, _len, _ref;
        err = ag.findError();
        result = [];
        _ref = ag.results;
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          arr = _ref[_i];
          result.push(arr[0]);
        }
        if (handler) return handler(err, result);
      });
      _results = [];
      for (_i = 0, _len = config.length; _i < _len; _i++) {
        item = config[_i];
        switch (item.type) {
          case 'create':
            _results.push(this.storage.create(item.stream, item.object, ag.fn, item.options));
            break;
          case 'update':
            _results.push(this.storage.update(item.stream, item.object, ag.fn, item.options));
            break;
          case 'remove':
            _results.push(this.storage.remove(item.stream, item.object, ag.fn, item.options));
            break;
          case 'removeCascade':
            _results.push(this.removeCascade(item.stream, item.object, ag.fn, item.options));
            break;
          case 'findOne':
            _results.push(this.findOne(item.stream, item.id, ag.fn));
            break;
          default:
            _results.push(ag._fn(null));
        }
      }
      return _results;
    };

    DataManager.prototype.save = function(stream, object, handler) {
      var _this = this;
      if (!object.id) {
        return this.storage.create(stream, object, function(err) {
          if (err) return handler(err);
          return handler(null, object);
        });
      } else {
        return this.storage.update(stream, object, function(err) {
          if (err) return handler(err);
          return handler(null, object);
        });
      }
    };

    DataManager.prototype.get = function(name, def) {
      return this.storage.db.get(name, def);
    };

    DataManager.prototype.is = function(name, def) {
      return this.storage.db.is(name, def);
    };

    DataManager.prototype.set = function(name, value) {
      return this.storage.db.set(name, value);
    };

    DataManager.prototype.sync = function(handler, progress_handler) {
      var _this = this;
      if (progress_handler == null) progress_handler = function() {};
      if (this.in_sync) return false;
      this.in_sync = true;
      this.on_sync.emit('start');
      return this.storage.sync(this.app, this.oauth, function(err, data) {
        _this.in_sync = false;
        _this.on_sync.emit('finish');
        if (!err && _this.timeout_id) _this.unschedule_sync(null);
        return handler(err, data);
      }, progress_handler);
    };

    DataManager.prototype.restore = function(files, handler) {
      var file, formData, i, url, xhr, _i, _len,
        _this = this;
      xhr = new XMLHttpRequest();
      url = '/rest/restore?';
      xhr.open('POST', this.oauth.getFullURL(this.app, url), true);
      formData = new FormData();
      i = 0;
      for (_i = 0, _len = files.length; _i < _len; _i++) {
        file = files[_i];
        formData.append("file" + i, file);
        i++;
      }
      xhr.onload = function(e) {
        log('Upload done', e, xhr.status);
        if (xhr.status !== 200) {
          return handler('HTTP error');
        } else {
          return handler(null);
        }
      };
      xhr.onerror = function(e) {
        log('XHR error', e, arguments);
        return handler('HTTP error');
      };
      return xhr.send(formData);
    };

    return DataManager;

  })();

  window.HTML5Provider = HTML5Provider;

  window.AirDBProvider = AirDBProvider;

  window.StorageProvider = StorageProvider;

  window.Lima1DataManager = DataManager;

  window.AirCacheProvider = AirCacheProvider;

  window.PhoneGapCacheProvider = PhoneGapCacheProvider;

  window.DesktopChannelProvider = DesktopChannelProvider;

  window.HTML5CacheProvider = HTML5CacheProvider;

  window.env = {
    mobile: false,
    prefix: ''
  };

}).call(this);
