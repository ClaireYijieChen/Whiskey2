(function() {
  var NetTransport, OAuthProvider, jQueryTransport,
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

  NetTransport = (function() {

    function NetTransport(uri) {
      this.uri = uri;
    }

    NetTransport.prototype.request = function(config, handler) {
      return handler('Not implemented');
    };

    return NetTransport;

  })();

  jQueryTransport = (function(_super) {

    __extends(jQueryTransport, _super);

    function jQueryTransport() {
      jQueryTransport.__super__.constructor.apply(this, arguments);
    }

    jQueryTransport.prototype.requests = 0;

    jQueryTransport.prototype.request = function(config, handler, options) {
      var request_finished, _ref, _ref2, _ref3,
        _this = this;
      this.requests++;
      if (this.requests === 1) this.on_start();
      request_finished = function(success) {
        _this.requests--;
        if (_this.requests <= 0) {
          _this.requests = 0;
          return _this.on_finish(success);
        }
      };
      if (options != null ? options.test : void 0) {
        log('Simulating:', config != null ? config.uri : void 0, config != null ? config.data : void 0);
        setTimeout(function() {
          request_finished(true);
          return handler(null, options != null ? options.test : void 0);
        }, 1000);
        return;
      }
      return $.ajax({
        type: (_ref = (config != null ? config.type : void 0)) != null ? _ref : 'GET',
        url: this.uri + (config != null ? config.uri : void 0),
        data: (_ref2 = (config != null ? config.data : void 0)) != null ? _ref2 : null,
        contentType: (_ref3 = (config != null ? config.contentType : void 0)) != null ? _ref3 : void 0,
        error: function(err, status, text) {
          var data, message, statusNo;
          message = text || 'HTTP error';
          statusNo = 500;
          if (err && err.status) statusNo = err.status;
          data = null;
          if (err && err.responseText) {
            try {
              data = JSON.parse(err.responseText);
            } catch (e) {

            }
          }
          log('jQuery error:', err, status, text, statusNo);
          request_finished(false);
          return handler({
            status: statusNo,
            message: message
          }, data);
        },
        success: function(data) {
          if (!data) return handler('No data');
          try {
            data = JSON.parse(data);
          } catch (e) {

          }
          request_finished(true);
          return handler(null, data);
        }
      });
    };

    jQueryTransport.prototype.on_start = function() {};

    jQueryTransport.prototype.on_finish = function() {};

    return jQueryTransport;

  })(NetTransport);

  OAuthProvider = (function() {

    function OAuthProvider(config, transport) {
      var _ref, _ref2, _ref3, _ref4, _ref5, _ref6, _ref7;
      this.config = config;
      this.transport = transport;
      this.tokenURL = (_ref = (_ref2 = this.config) != null ? _ref2.tokenURL : void 0) != null ? _ref : '/token';
      this.clientID = (_ref3 = (_ref4 = this.config) != null ? _ref4.clientID : void 0) != null ? _ref3 : 'no_client_id';
      this.scope = (_ref5 = (_ref6 = this.config) != null ? _ref6.scope : void 0) != null ? _ref5 : 'app';
      this.token = (_ref7 = this.config) != null ? _ref7.token : void 0;
    }

    OAuthProvider.prototype.getFullURL = function(app, path) {
      return this.transport.uri + ("" + path + "app=" + app + "&oauth_token=" + this.token);
    };

    OAuthProvider.prototype.rest = function(app, path, body, handler, options) {
      var _this = this;
      return this.transport.request({
        uri: "" + path + "app=" + app + "&oauth_token=" + this.token,
        type: body ? 'POST' : 'GET',
        data: body,
        contentType: body ? 'text/plain' : null
      }, function(error, data) {
        if (error) {
          if (error.status === 401) _this.on_token_error(null);
          return handler(error.message);
        }
        return handler(null, data);
      }, options);
    };

    OAuthProvider.prototype.tokenByUsernamePassword = function(username, password, handler) {
      var url,
        _this = this;
      url = this.tokenURL;
      return this.transport.request({
        uri: url,
        type: 'POST',
        data: {
          username: username,
          password: password,
          client_id: this.clientID,
          grant_type: 'password',
          scope: this.scope
        }
      }, function(error, data) {
        log('Response:', error, data);
        if (error) return handler(data != null ? data : error);
        _this.token = data.access_token;
        _this.on_new_token(_this.token);
        return handler(null, data);
      });
    };

    OAuthProvider.prototype.on_token_error = function() {};

    OAuthProvider.prototype.on_new_token = function() {};

    return OAuthProvider;

  })();

  window.jQueryTransport = jQueryTransport;

  window.OAuthProvider = OAuthProvider;

}).call(this);
