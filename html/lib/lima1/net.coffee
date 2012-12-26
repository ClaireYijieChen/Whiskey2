class NetTransport

  constructor: (@uri) ->

  request: (config, handler) ->
    handler 'Not implemented'

class jQueryTransport extends NetTransport

  requests: 0

  request: (config, handler, options) ->
    # log 'Doing request', @uri, config?.uri, config?.type, config?.data
    @requests++
    if @requests is 1
      @on_start()
    request_finished = (success) =>
      @requests--
      if @requests<=0
        @requests = 0
        @on_finish(success)
    if options?.test
      log 'Simulating:', config?.uri, config?.data
      setTimeout =>
        request_finished(yes)
        handler(null, options?.test)
      , 1000
      return
    $.ajax({
      type: (config?.type) ? 'GET'
      url: @uri+config?.uri
      data: (config?.data) ? null
      contentType: (config?.contentType) ? undefined
      error: (err, status, text) =>
        message = text or 'HTTP error'
        statusNo = 500
        if err and err.status
          statusNo = err.status
        data = null
        if err and err.responseText
          try
            data = JSON.parse err.responseText
          catch e
        log 'jQuery error:', err, status, text, statusNo
        request_finished(no)
        handler {status: statusNo, message: message}, data
      success: (data) =>
        if not data then return handler 'No data'
        try
          data = JSON.parse data
        catch e
        request_finished(yes)
        handler null, data
    })

  on_start: ->

  on_finish: ->

class OAuthProvider

  constructor: (@config, @transport) ->
    @tokenURL = @config?.tokenURL ? '/token'
    @clientID = @config?.clientID ? 'no_client_id'
    @scope = @config?.scope ? 'app'
    @token = @config?.token

  getFullURL: (app, path) ->
    return @transport.uri+"#{path}app=#{app}&oauth_token=#{@token}"

  rest: (app, path, body, handler, options) ->
    @transport.request {
      uri: "#{path}app=#{app}&oauth_token=#{@token}"
      type: if body then 'POST' else 'GET'
      data: body
      contentType: if body then 'text/plain' else null
    }, (error, data) =>
      # log 'Rest response:', error, data
      if error
        if error.status is 401
          @on_token_error null
        return handler error.message
      handler null, data
    , options


  tokenByUsernamePassword: (username, password, handler) ->
    url = @tokenURL
    @transport.request {
      uri: url
      type: 'POST'
      data: {
        username: username
        password: password
        client_id: @clientID
        grant_type: 'password'
        scope: @scope
      }
    }, (error, data) =>
      log 'Response:', error, data
      if error then return handler data ? error
      @token = data.access_token
      @on_new_token @token
      handler null, data

  on_token_error: () ->

  on_new_token: () ->


window.jQueryTransport = jQueryTransport
window.OAuthProvider = OAuthProvider
