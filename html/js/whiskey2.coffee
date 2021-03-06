yepnope({
  load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/whiskey2.css', 'lib/lima1/net.js', 'lib/lima1/main.js', 'bs/js/bootstrap-datepicker.js', 'bs/css/datepicker.css', 'lib/common-web/canto-0.15.js', 'js/w2plugins.js'],
  complete: ->
    $(document).ready(->
      app = new Whiskey2
      app.start()
    );
})

class Whiskey2
  start: ->
    db = new HTML5Provider 'app.db', '1'
    storage = new StorageProvider db
    # 'http://localhost:8888'
    jqnet = new jQueryTransport 'https://lima1-kvj.rhcloud.com'
    @oauth = new OAuthProvider {
      clientID: 'whiskey2web'
    }, jqnet
    @manager = new Lima1DataManager 'whiskey2', @oauth, storage
    @syncProgress = $(document.createElement('div')).addClass('progress progress-striped active')
    @syncProgressBar = $(document.createElement('div')).addClass('bar').appendTo(@syncProgress)
    @manager.on_sync.on 'start', =>
      @syncAlert = @showAlert 'Please stand by...', persistent: yes, content: @syncProgress
    @manager.on_sync.on 'finish', =>
      @syncAlert.remove()
    @manager.on_scheduled_sync = () =>
      @sync()
    @oauth.token = @manager.get('token', 'no-value')
    @templateConfigs = {}
    @sheetPlugins = {}
    @templateDrawer = new DrawTemplate this
    for key, cls of templateConfigs
      @templateConfigs[key] = new cls this
    for key, cls of sheetPlugins
      @sheetPlugins[key] = new cls(this, key)
    #@templateConfigs.week = new WeekTemplateConfig this
    #@templateConfigs.grid = new GridTemplateConfig this
    #@templateConfigs.chronodex = new ChronodexConfig this
    @manager.open (error) =>
      if error
        @manager = null
        @showError error
      @manager.storage.cache = new HTML5CacheProvider(@oauth, @manager.app, 1280)
      @showAlert 'Application started', severity: 'success'
      @oauth.on_token_error = =>
        @showLoginDialog()
      @bindMain()
      @refreshBookmarks () =>
        @refreshNotepads()
        @refreshTemplates()
      @manager.start_ping (err, haveData) =>
        if haveData then @sync =>
          if @selected then @selected.reloadSheets()
      @sync =>
        notepadID = null
        if @selected then return
        for id of @notepads
          notepadID = id
          break
        if notepadID then @selectNotepad notepadID

  showLoginDialog: ->
    $('#main-password').val('')
    $('#main-login-dialog').modal('show')

  bindMain: ->
    templatesManager = new TemplateManager(this)
    @templatesManager = templatesManager
    $('#main-sync').bind 'click', =>
      @sync()
    $('#main-do-login').bind 'click', =>
      @doLogin()
    $('#main-do-login-close').bind 'click', =>
      $('#main-login-dialog').modal('hide')
    $('#main-add-notepad').bind 'click', =>
      $('#main-add-notepad-dialog').modal('show')
      $('#main-add-notepad-name').val('')
    $('#main-do-add-notepad').bind 'click', =>
      @doAddNotepad()
    $('#main-tab-templates a').bind 'click', ->
      $(this).tab('show')
      templatesManager.refresh()

  dragSetType: (e, type, value) ->
    if e?.originalEvent?.dataTransfer?.setData
      e.originalEvent.dataTransfer.setData type, JSON.stringify(value)

  dragGetType: (e, type) ->
    # log 'getType:', e?.originalEvent?.dataTransfer, e?.originalEvent?.dataTransfer.files
    if 'Files' is type and e?.originalEvent?.dataTransfer?.files
      return e.originalEvent.dataTransfer.files
    if e?.originalEvent?.dataTransfer?.getData
      try
        return JSON.parse(e?.originalEvent?.dataTransfer?.getData(type))
      catch e
        return null
    return null

  dragHasType: (e, types...) ->
    if not e?.originalEvent?.dataTransfer?.types then return no
    for type in e?.originalEvent?.dataTransfer?.types
      for t in types
        if type is t then return yes
    return no

  dragGetOffset: (e, div) ->
    offset = left: e.originalEvent.clientX, top: e.originalEvent.clientY
    if div
      offset.left -= div.offset().left
      offset.top -= div.offset().top
    return offset

  refreshTemplates: (handler) ->
    @manager.storage.select 'templates', [], (err, data) =>
      if err
        if handler then handler(err)
        return
      @templates = {}
      for item in data
        @templates[item.id] = item
      if handler then handler()

  refreshBookmarks: (handler) ->
    @manager.storage.select 'bookmarks', [], (err, data) =>
      if err then return handler(err)
      @bookmarks = {}
      for item in data
        if not @bookmarks[item.sheet_id]
          @bookmarks[item.sheet_id] = []
        @bookmarks[item.sheet_id].push item
      handler()

  refreshNotepads: (selectID) ->
    @manager.storage.select 'notepads', ['archived', {op: '<>', var: 1}], (err, arr) =>
      if err then return @showError err
      # log 'Notepads:', arr
      $('#main-tabs li.main-tab-notepad').remove()
      $('#main-tabs-content .main-tab-notepad').remove()
      @notepads = {}
      @selected = null
      @sortWithNext arr
      moveNotepad = (index, id) =>
        @moveWithNext 'notepads', arr, index, [id], (err) =>
          if err then return @showError err
          @refreshNotepads selectID
      for i in [0...arr.length]
        item = arr[i]
        li = $(document.createElement('li')).addClass('main-tab-notepad')
        a = $(document.createElement('a')).attr(href: '#np'+item.id).text(' '+item.name).appendTo li
        a.prepend('<i class="icon-book"></i>')
        li.insertBefore $('#main-tab-templates')
        div = $(document.createElement('div')).addClass('tab-pane main-tab-notepad').attr(id: 'np'+item.id)
        $('#main-tabs-content').append(div)
        do (a, item, i) =>
          a.bind 'dragstart', (e) =>
            @dragSetType e, 'custom/notepad', {id: item.id}
          a.bind 'dragover', (e) =>
            if (@dragHasType e, 'custom/note') or (@dragHasType e, 'custom/sheet') or (@dragHasType e, 'custom/notepad')
              e.preventDefault()
          a.bind 'dragenter', (e) =>
            if @dragHasType e, 'custom/notepad'
              e.preventDefault()
            if (@dragHasType e, 'custom/note') or (@dragHasType e, 'custom/sheet') or (@dragHasType e, 'custom/bmark')
              a.tab('show')
              e.preventDefault()
          a.bind 'drop', (e) =>
            notepadData = @dragGetType e, 'custom/notepad'
            if notepadData
              moveNotepad i, notepadData.id
              e.stopPropagation()
          a.bind 'click', (e) =>
            e.preventDefault()
            @selectNotepad(item.id)
        n = @renderNotepad div, item
        n.anchor = a
        if item.id is selectID
          @selectNotepad(item.id)

  selectNotepad: (id) ->
    n = @notepads[id]
    if not n then return @showError 'Notepad not found'
    n.anchor.tab('show')
    n.reloadSheets()
    @selected = n

  doAddNotepad: ->
    name = $('#main-add-notepad-name').val().trim()
    if not name then return @showError 'Name is empty'
    $('#main-add-notepad-dialog').modal('hide')
    @manager.storage.create 'notepads', {name: name, archived: 0}, (err, data) =>
      if err then return @showError err
      $('#main-add-notepad-dialog').modal('hide')
      @showAlert 'Notepad created', severity: 'success'
      @refreshNotepads data.id

  renderNotepad: (parent, notepad) ->
    div = $('#main-notepad-template').clone().appendTo(parent).removeClass('hide')
    n = new Notepad @, notepad, div
    @notepads[notepad.id] = n
    return n

  doLogin: ->
    login = $('#main-login').val().trim()
    password = $('#main-password').val().trim()
    log 'Checking', login, password
    $('#main-login-dialog').modal('hide')
    @oauth.tokenByUsernamePassword login, password, (err) =>
      if err
        @showLoginDialog()
        return @showError err.error_description
      @showAlert 'Login OK', severity: 'success'
      @sync()

  sync: (handler) ->
    if not @manager then return
    # @showAlert 'Sync started'
    @manager.sync (err) =>
      if err then return @showError err
      @refreshTemplates =>
        if handler then handler()
    , (type) =>
      w = 100
      switch type
        when 0 then w = 25
        when 1 then w = 50
        when 2 then w = 75
      # log 'Sync progress', w, type
      @syncProgressBar.width "#{w}%"

  showError: (message) ->
    @showAlert message, severity: 'error', 'Error'

  showPrompt: (message, handler) ->
    div = $(document.createElement('p'))
    button = $(document.createElement('button')).addClass('btn btn-danger').text('Proceed').appendTo(div)
    button.bind 'click', (e) =>
      if handler then handler()
      alert.remove()
    alert = @showAlert message, persistent: yes, severity: 'block', content: div, 'Prompt'

  showAlert: (message, config, title = 'Whiskey2') ->
    div = $(document.createElement('div')).appendTo($('#alerts')).addClass('alert alert-'+(config?.severity ? 'info'));
    $(document.createElement('button')).appendTo(div).addClass('close').attr({'data-dismiss': 'alert'}).html('&times;');
    $(document.createElement('h4')).appendTo(div).text(title ? 'Untitled');
    $(document.createElement('span')).appendTo(div).text(message);
    if config?.content
      div.append(config.content)
    if not config?.persistent
      setTimeout =>
        div.remove();
      , config?.timeout ? 3000
    return div

  emptyTemplate: {
    width: 102
    height: 144
    name: 'No template'
  }
  getTemplate: (id) ->
    tmpl = @templates[id]
    if tmpl then return tmpl
    return @emptyTemplate

  getTemplateConfig: (tmpl) ->
    return @templateConfigs[tmpl?.type] ? null


  findByID: (arr, id, attr = 'id') ->
    if not id then return -1
    for i in [0...arr.length]
      if arr[i][attr] is id then return i
    return -1

  addUnique: (array, item, attr = 'id') ->
    # Adds element only when it's not here
    index = @findByID array, item[attr], attr
    if index is -1 then array.push item

  removeWithNext: (sink, array, items) ->
    # Removes items and adds updated elements to array
    for item in items
      left_index = @findByID array, item.id, 'next_id' # someone refers to me?
      if left_index isnt -1
        # someone refer to me
        right_index = @findByID array, item.next_id # I refer to someone?
        if right_index isnt -1
          array[left_index].next_id = item.next_id
        else
          array[left_index].next_id = null
        @addUnique sink, array[left_index]

  addWithNext: (sink, array, after, items) ->
    # Adds items to array and updates next_id. if after is -1 - add to the begin
    if not items?.length then return
    next_id = null
    right_index = 0
    if after isnt -1
      # have after - fill next_id and update array[after]
      right_index = after+1
      next_id = array[after].next_id # save next_id
      array[after].next_id = items[0].id
      @addUnique sink, array[after] # after refers to first element
    else
      # add to the top - save firtst id as next_id
      if array.length>0 then next_id = array[0].id
    for i in [0...items.length-1]
      items[i].next_id = items[i+1].id
    items[items.length-1].next_id = next_id # link last item to next_id

  sortWithNext: (items) ->
    if not items?.length then return items
    i = 0
    while i<items.length
      item = items[i]
      # Have next_id
      index = @findByID items, item.next_id
      # log 'sortWithNext', i, item, index
      if index isnt -1
        # found
        found = items[index]
        if index<i
          # left
          items.splice i+1, 0, found
          items.splice index, 1
          i-- # Check one more time
        else
          if index>i+1
            # right (not same and not next)
            items.splice index, 1
            items.splice i+1, 0, found
      i++ # next item
    return items

  moveWithNext: (stream, array, index, ids, handler, adapter) ->
    # log 'moveWithNext', index, ids, array
    ag = new AsyncGrouper ids.length, () =>
      err = ag.findError()
      # log 'Search sheets', err, ag.results
      if err then return handler err
      items = []
      sink = []
      for arr in ag.results
        items.push arr[0]
        sink.push arr[0]
      # log 'Before remove', items, sink
      @removeWithNext sink, array, items
      # log 'After remove', items, sink
      @addWithNext sink, array, index, items
      # log 'After add', items, sink
      config = []
      for item in sink
        if adapter then adapter(item)
        config.push type: 'update', stream: stream, object: item
      @manager.batch config, (err) =>
        # log 'After update', err
        if err then return handler err
        handler null
    for id in ids
      @manager.findOne stream, id, ag.fn

  sortTemplates: (arr) ->
    result = []
    tag = null
    section = null
    for item in arr
      if item.tag isnt tag
        # Tag changed
        section = title: (if item.tag then item.tag else 'No tag'), items: []
        tag = item.tag
        result.push section
      section.items.push item
    return result

class Notepad

  zoom: 1
  zoomStep: 0.1

  constructor: (@app, @notepad, @div) ->
    @div.find('.notepad-name').text(@notepad.name)
    @divMiniatures = div.find('.notepad-sheets')
    @divContent = div.find('.notepad-sheets-wrap')
    @div.find('.notepad-add-sheet').bind 'click', () =>
      @showSheetDialog {}, =>
        @reloadSheets()
    @div.find('.notepad-reload-sheets').bind 'click', =>
      @reloadSheetsBookmarks()
    @div.find('.notepad-zoom-in').bind 'click', =>
      @zoomInOut 1
    @div.find('.notepad-zoom-out').bind 'click', =>
      @zoomInOut -1
    @initSheetPlaceholders()
    @reloadSheets()

  reloadSheetsBookmarks: ->
    @app.refreshBookmarks () =>
      @reloadSheets()

  zoomInOut: (direction = 1) ->
    if direction<0 and @zoom<=@zoomStep then return
    @zoom += direction*@zoomStep
    @divContent.css 'font-size': "#{@zoom}em"
    @reloadSheets()

  showBMarkDialog: (bmark, handler) ->
    $('#bmark-dialog').modal('show')
    $('#bmark-dialog-name').val(bmark.name ? 'Untitled').focus()
    $('#bmark-dialog-color').val(bmark.color ? '#ff0000')
    $('#do-remove-bmark-dialog').unbind('click').bind 'click', (e) =>
      @app.showPrompt 'Are you sure want to remove bookmark?', =>
        @app.manager.removeCascade 'bookmarks', bmark, (err) =>
          if err then return @app.showError 'Error removing bookmark'
          if handler then handler bmark
      $('#bmark-dialog').modal('hide')
    $('#do-save-bmark-dialog').unbind('click').bind 'click', (e) =>
      name = $('#bmark-dialog-name').val().trim()
      color = $('#bmark-dialog-color').val()
      if not name then return @app.showError 'Name is empty'
      log 'Save bmark', bmark, name, color
      bmark.name = name
      bmark.color = color
      _handler = (err, data) =>
        if err then return @app.showError err
        @app.showAlert 'Bookmark saved', severity: 'success'
        $('#bmark-dialog').modal('hide')
        if handler then handler bmark
      @app.manager.save 'bookmarks', bmark, _handler

  showSheetDialog: (sheet, handler) ->
    noTemplateText = 'No template'
    $('#sheet-dialog').modal('show')
    templateID = sheet.template_id ? null
    $('#sheet-dialog-name').val(sheet.title ? '').focus().select()
    pluginsText = $('#sheet-dialog-plugins')
    pluginsText.val(if sheet.plugins then sheet.plugins.join(',') else '')
    $('#do-remove-sheet-dialog').unbind('click').bind 'click', (e) =>
      if not sheet.id then return
      @app.showPrompt 'Are you sure want to remove sheet? It will remove notes also', =>
        @removeSheets [sheet.id]
      $('#sheet-dialog').modal('hide')
      return no
    $('#do-save-sheet-dialog').unbind('click').bind 'click', (e) =>
      name = $('#sheet-dialog-name').val().trim()
      if not name then return @app.showError 'Title is empty'
      log 'Save sheet', name, templateID
      pluginsArr = pluginsText.val().split(',')
      sheet.plugins = []
      for item in pluginsArr
        if item.trim() then sheet.plugins.push(item.trim())
      if sheet.plugins.length is 0
        delete sheet.plugins
      sheet.template_id = templateID
      sheet.title = name
      _handler = (err, data) =>
        if err then return @app.showError err
        @app.showAlert 'Sheet saved', severity: 'success'
        $('#sheet-dialog').modal('hide')
        if handler then handler sheet
      if not sheet.id
        sheet.notepad_id = @notepad.id
        @app.manager.storage.create 'sheets', sheet, _handler
      else
        @app.manager.storage.update 'sheets', sheet, _handler
      return no
    templateButtonText = $('#sheet-dialog-template-title')
    templateButtonText.text(noTemplateText)
    ul = $('#sheet-dialog-template-menu').empty()
    @app.manager.storage.select 'templates', [], (err, data) =>
      if err then return
      data = @app.sortTemplates data
      ul.empty()
      li = $(document.createElement('li')).appendTo(ul)
      a = $(document.createElement('a')).appendTo(li)
      a.text noTemplateText
      a.bind 'click', (e) =>
        templateID = null
        templateButtonText.text(noTemplateText)
      for section in data
        li = $(document.createElement('li')).addClass('dropdown-submenu').appendTo(ul)
        a = $(document.createElement('a')).appendTo(li)
        a.text section.title
        ul2 = $(document.createElement('ul')).addClass('dropdown-menu').appendTo(li)
        for item in section.items
          li = $(document.createElement('li')).appendTo(ul2)
          a = $(document.createElement('a')).appendTo(li)
          a.text item.name
          do (a, item) =>
            a.bind 'click', (e) =>
              templateID = item.id
              templateButtonText.text(item.name)
          if templateID is item.id
            templateButtonText.text(item.name)
    , {order: ['tag', 'name']}

  removeSheets: (ids) ->
    sink = []
    items = []
    config = []
    for id in ids
      config.push stream: 'sheets', type: 'removeCascade', object: {id: id}
      items.push id: id
    @app.removeWithNext sink, @sheets, items
    for item in sink
      config.push type: 'update', stream: 'sheets', object: item
    @app.manager.batch config, (err) =>
      if err then return @app.showError err
      @reloadSheets()

  moveSheets: (index, data) ->
    @app.moveWithNext 'sheets', @sheets, index, data.ids, (err) =>
      if err then return @app.showError err
      @reloadSheets()
    , (item) =>
      item.notepad_id = @notepad.id

  moveBookmark: (id, sheet, handler) ->
    @app.manager.findOne 'bookmarks', id, (err, bmark) =>
      if err then return @app.showError err
      bmark.sheet_id = sheet.id
      bmark.notepad_id = @notepad.id
      @app.manager.save 'bookmarks', bmark, (err) =>
        if err then return @app.showError err
        @reloadSheetsBookmarks()

  renderBookmark: (bmark, zoom, handler) ->
    color = bmark.color ? '#ff0000'
    div = $(document.createElement('div')).addClass('bookmark')
    div.css 'border-color': color, 'border-bottom-color': 'transparent', 'font-size': "#{zoom}em"
    div.attr draggable: yes, rel: 'tooltip', title: bmark.name
    div.tooltip placement: 'bottom'
    div.bind 'dblclick', (e) =>
      e.preventDefault()
      e.stopPropagation()
      if handler then handler(bmark)
      return no
    div.bind 'dragstart', (e) =>
      @app.dragSetType e, 'custom/bmark', {id: bmark.id}
    return div

  renderSheetMiniatures: (sheets) ->
    div = @divMiniatures
    div.empty()
    @selectedSheets = {}
    if not sheets?.length then return
    minHeight = 3
    maxHeight = 30
    oneItemHeight = 50
    height = $(window).height() - 180
    step = maxHeight
    if sheets.length>1
      step = Math.floor((height-oneItemHeight)/(sheets.length-1))
    if step>maxHeight then step = maxHeight
    if step<minHeight then step = minHeight
    h = 0
    renderBookmarks = (i, sheet, divItem) =>
      arr = @app.bookmarks[sheet.id]
      if not arr then return
      bmarkx = 0
      bmarky = 0
      bmarkstep = 1
      for item in arr
        divBMark = @renderBookmark item, 0.5, (bmark) =>
          @showBMarkDialog bmark, () =>
            @reloadSheetsBookmarks()
        divBMark.css top: "#{bmarkx}em", right: "#{bmarky}em"
        divItem.append divBMark
        bmarkx += bmarkstep
        bmarky -= bmarkstep

    for i in [0...sheets.length]
      sheet = sheets[i]
      divItem = $(document.createElement('div')).addClass('notepad-sheet-miniature')
      divItem.attr draggable: yes
      divItem.css top: "#{h}px"
      divItem.appendTo div
      divItemText = $(document.createElement('div')).addClass('notepad-sheet-miniature-title').appendTo divItem
      divItemText.text sheet.title
      h += step
      do (i, sheet, divItem) =>
        renderBookmarks i, sheet, divItem
        divItem.bind 'click', (e) =>
          e.preventDefault()
          if e.ctrlKey
            if @selectedSheets[sheet.id]
              # Unselect
              delete @selectedSheets[sheet.id]
              divItem.removeClass 'notepad-sheet-miniature-selected'
            else
              @selectedSheets[sheet.id] = yes
              divItem.addClass 'notepad-sheet-miniature-selected'
            return
          div.find('.notepad-sheet-miniature-selected').removeClass 'notepad-sheet-miniature-selected'
          @selectedSheets = {}
          @loadSheets i
        divItem.bind 'dragstart', (e) =>
          if @selectedSheets[sheet.id]
            ids = []
            for id of @selectedSheets
              ids.push id
            @app.dragSetType e, 'custom/sheet', {ids: ids}
          else
            @app.dragSetType e, 'custom/sheet', {ids: [sheet.id]}
        divItem.bind 'dragover', (e) =>
          if @app.dragHasType e, 'custom/sheet'
            e.preventDefault()
          return no
        divItem.bind 'drop', (e) =>
          bmark = @app.dragGetType e, 'custom/bmark'
          if bmark
            @moveBookmark bmark.id, sheet
            e.stopPropagation()
            return no
          sheetsData = @app.dragGetType e, 'custom/sheet'
          if sheetsData
            @moveSheets i, sheetsData
            e.stopPropagation()
          return no
        divItem.bind 'mouseover', (e) =>
          divItem.addClass('notepad-sheet-miniature-hover')
        divItem.bind 'dragenter', (e) =>
          #custom/bmark
          divItem.addClass('notepad-sheet-miniature-hover')
          if @app.dragHasType e, 'custom/bmark'
            e.preventDefault()
          if @app.dragHasType e, 'custom/note'
            @loadSheets i
            e.preventDefault()
        divItem.bind 'mouseout', (e) =>
          divItem.removeClass('notepad-sheet-miniature-hover')
        divItem.bind 'dragleave', (e) =>
          divItem.removeClass('notepad-sheet-miniature-hover')
    $(document.createElement('div')).addClass('clear').appendTo(div)

  reloadSheets: () ->
    @app.manager.storage.select 'sheets', ['notepad_id', @notepad.id], (err, sheets) =>
      if err then return @app.showError err
      @sheets = @app.sortWithNext sheets
      @renderSheetMiniatures @sheets
      index = @app.findByID @sheets, @lastSheetID
      if index is -1 and @sheets.length>0
        index = 0
      if index isnt -1 then @loadSheets index

  maxSheetsVisible: 2

  initSheetPlaceholders: () ->
    @sheetDivs = []
    for i in [0...@maxSheetsVisible]
      div = $(document.createElement('div')).addClass('sheet').appendTo @divContent
      divContent = $(document.createElement('div')).addClass('sheet_content').appendTo div
      canvas = $(document.createElement('canvas')).addClass('sheet-canvas').appendTo divContent
      divTitle = $(document.createElement('div')).addClass('sheet_title').appendTo div
      divToolbar = $('#sheet-toolbar-template').clone().removeClass('hide').appendTo div
      @sheetDivs.push div
      div.hide()
      if i is 0
        @spiralDiv = $(document.createElement('div')).addClass('sheet-spiral').appendTo @divContent

  loadSheets: (index) ->
    @selectedNotes = {}
    @lastSheetID = @sheets[index]?.id
    count = 0
    @notes = []
    NO_SPIRAL = 0
    RIGHT_SPIRAL = 1
    CENTER_SPIRAL = 2
    spiralType = NO_SPIRAL
    spiralHeight = -1
    sheetsVisible = 0
    if index+@maxSheetsVisible >= @sheets.length
      index = @sheets.length - @maxSheetsVisible
      if index<0 then index = 0
    while count<@maxSheetsVisible
      div = @sheetDivs[count]
      if not @sheets[index]
        div.hide()
      else
        div.show()
        sheetsVisible++
        tmpl = @loadSheet(index, div)
        if div.height()<spiralHeight or spiralHeight<0 then spiralHeight = div.height()
      index++
      count++
    if sheetsVisible is 1 then spiralType = RIGHT_SPIRAL
    else if sheetsVisible > 1 then spiralType = CENTER_SPIRAL
    spiralHeight = ""+(Math.floor(spiralHeight/18)*18)+"px"
    switch spiralType
      when NO_SPIRAL
        @spiralDiv.hide()
      when RIGHT_SPIRAL
        @spiralDiv.show().height(spiralHeight).attr('class': 'sheet-spiral spiral-right')
      when CENTER_SPIRAL
        @spiralDiv.show().height(spiralHeight).attr('class': 'sheet-spiral spiral-center')


  showNotesDialog: (sheet, note, handler) ->
    $('#note-dialog').modal backdrop: 'static', keyboard: no
    currentText = note.text ? ''
    $('#note-dialog-text').val(currentText).focus()
    $('#note-dialog-collapsed').attr 'checked': if note.collapsed then yes else no
    colors = $('#note-dialog-colors').empty()
    widths = $('#note-dialog-widths').empty()
    currentColor = note.color ? (@lastColor ? 0)
    currentWidth = note.width ? (@lastWidth ? @noteWidths[@noteDefaultWidth])
    for color in [0...@colors]
      a = $(document.createElement('a')).addClass('btn btn-small').attr(href: '#').data('index', color)
      a.appendTo colors
      if currentColor is color
        a.addClass 'active'
      adiv = $(document.createElement('div')).addClass('note-color-button note-color'+color).html('&nbsp;').appendTo(a)
    colors.button()
    for width in @noteWidths
      a = $(document.createElement('a')).addClass('btn').attr(href: '#').data('width', width)
      a.text("#{width}%").appendTo widths
      if currentWidth is width
        a.addClass 'active'
    widths.button()
    $('#do-close-note-dialog').unbind('click').bind 'click', (e) =>
      text = $('#note-dialog-text').val().trim()
      if text isnt currentText
        @app.showPrompt 'There is unsaved text. Are you sure want to close dialog?', =>
          $('#note-dialog').modal('hide')
      else
        $('#note-dialog').modal('hide')

    $('#do-remove-note-dialog').unbind('click').bind 'click', (e) =>
      if not note.id then return
      @app.showPrompt 'Are you sure want to remove note?', () =>
        @app.manager.storage.remove 'notes', note, (err) =>
          if err then return @app.showError err
          $('#note-dialog').modal('hide')
          @reloadSheets()
    $('#do-save-note-dialog').unbind('click').bind 'click', (e) =>
      text = $('#note-dialog-text').val().trim()
      collapsed = $('#note-dialog-collapsed').attr 'checked'
      color = colors.find('a.active').data('index')
      width = widths.find('a.active').data('width')
      log 'Save', collapsed, color, width
      if not text then return @app.showError 'Text is empty'
      note.text = text
      note.color = color
      note.width = width
      note.collapsed = if collapsed then yes else no
      _handler = (err, data) =>
        if err then return @app.showError err
        @app.showAlert 'Note saved', severity: 'success'
        $('#note-dialog').modal('hide')
        if handler then handler(note)
      if not note.id
        @lastColor = note.color
        @lastWidth = note.width
        note.sheet_id = sheet.id
        @app.manager.storage.create 'notes', note, _handler
      else
        @app.manager.storage.update 'notes', note, _handler

  resetNoteSelection: ->
    @selectedNotes = {}
    @divContent.find('.note-selected').removeClass('note-selected')

  preciseEm: (value) ->
    return Math.floor(value*100/@zoomFactor)/100

  createSpotLink: (id, spot) ->
    @app.manager.findOne 'notes', id, (err, note) =>
      if err then return @app.showError err
      links = note.links
      if not links
        note.links = []
        links = note.links
      index = @app.findByID links, spot.id, 'spot'
      if index isnt -1
        log 'Spot already created'
        return
      links.push(spot: spot.id)
      @app.manager.save 'notes', note, (err) =>
        if err then return @app.showError err
        @reloadSheets()
    return yes

  createNoteLink: (note, noteID) ->
    if note.id is noteID
      log 'Same note'
      return no
    index = @app.findByID @notes, noteID
    if -1 is index
      log 'From other page'
      return no
    otherNote = @notes[index]
    if note.sheet_id isnt otherNote.sheet_id
      log 'From other sheet'
      return no
    links = otherNote.links
    if not links
      otherNote.links = []
      links = otherNote.links
    index = @app.findByID links, note.id
    if index isnt -1
      log 'Already created'
      return no
    links.push(id: note.id)
    @app.manager.save 'notes', otherNote, (err) =>
      if err then return @app.showError err
      @reloadSheets()
    return yes

  loadNotes: (sheet, parent, canvas, zoom) ->
    createSpots = (note) =>
      spots = sheet.config?.spots ? []
      links = note.links ? []
      for spot in spots
        spotDiv = $(document.createElement('div')).addClass('note-spot').appendTo(parent)
        x = @preciseEm spot.x
        y = @preciseEm spot.y
        spotDiv.css left: "#{x}em", top: "#{y}em"
        do (spot, spotDiv) =>
          spotDiv.bind 'dragover', (e) =>
            if @app.dragHasType e, 'custom/note'
              spotDiv.addClass('note-spot-hover')
              e.preventDefault()
          spotDiv.bind 'drop', (e) =>
            noteData = @app.dragGetType e, 'custom/note'
            if noteData?.id and noteData?.link
              if @createSpotLink noteData.id, spot
                e.stopPropagation()
                return no
          spotDiv.bind 'dragleave', (e) =>
            spotDiv.removeClass('note-spot-hover')
          spotDiv.bind 'dragend', (e) =>
            spotDiv.remove()
    renderArrow = (box1, box2, color) =>
      triangleSize = 2*zoom
      x1 = box1.x+box1.w/2
      x2 = box2.x+box2.w/2
      y1 = box1.y+box1.h/2
      y2 = box2.y+box2.h/2
      difx = Math.abs(x2-x1)
      dify = Math.abs(y2-y1)
      verticalTriangle = difx>dify
      if verticalTriangle
        xt1 = x1
        xt2 = x1
        yt1 = y1-triangleSize
        yt2 = y1+triangleSize
      else
        xt1 = x1-triangleSize
        xt2 = x1+triangleSize
        yt1 = y1
        yt2 = y1
      canvas.beginPath().moveTo(xt1, yt1).lineTo(xt2, yt2).lineTo(x2, y2).fill(fillStyle: color).endPath()

    renderLinks = (notes) =>
      dotsRadius = 2
      spots = sheet.config?.spots ? []
      for spot in spots
        spotDiv = $(document.createElement('div')).addClass('note-spot-place').appendTo(parent)
        spotDiv.attr(id: "spot#{sheet.id}#{spot.id}")
        x = @preciseEm(spot.x)
        y = @preciseEm(spot.y)
        spotDiv.css(left: "#{x}em", top: "#{y}em")
      for note in notes
        links = note.links ? []
        if links.length is 0 then continue
        dotsDiv = $(document.createElement('div')).addClass('note-dots').appendTo(parent)
        x = @preciseEm note.x-2*dotsRadius
        y = @preciseEm note.y
        radius = @preciseEm dotsRadius
        dotsDiv.css left: "#{x}em", top: "#{y}em"
        spots = sheet.config?.spots ? []
        div2box = (div) =>
          return x: div.position().left, y: div.position().top, w: div.outerWidth(), h: div.outerHeight()
        box1 = div2box($('#note'+note.id))
        for i in [0...links.length]
          link = links[i]
          index = @app.findByID notes, link.id
          createLink = yes
          other = null
          if index is -1
            createLink = no
            if link.spot
              spotIndex = @app.findByID spots, link.spot
              if spotIndex isnt -1
                spot = spots[spotIndex]
                createLink = yes
                box2 = div2box($("#spot#{sheet.id}#{spot.id}"))
          else
            other = notes[index]
            if other.sheet_id isnt note.sheet_id
              createLink = no
            else
              box2 = div2box($('#note'+other.id))
          if createLink
            renderArrow box1, box2, '#ffaaaa', 6
          dotDiv = $(document.createElement('div')).addClass('note-dot').appendTo(dotsDiv)
          dotDiv.css 'border-width': "#{radius}em", 'border-radius': "#{radius}em"
          dotDiv.css 'border-color': (if createLink then '#008800' else '#ff0000')
          do (i, link, note) =>
            dotDiv.bind 'dblclick', (e) =>
              @app.showPrompt 'Are you sure want to remove link?', =>
                note.links.splice(i, 1)
                @app.manager.save 'notes', note, (err) =>
                  if err then return @app.showError err
                  @reloadSheets()
              e.stopPropagation()
              e.preventDefault()
              return no

    loadNote = (note) =>
      mergeNotes = (ids) =>
        config = [type: 'findOne', id: note.id, stream: 'notes']
        for id in ids
          config.push type: 'findOne', id: id, stream: 'notes'
        @app.manager.batch config, (err, arr) =>
          if err then return @app.showError err
          n = arr[0]
          for i in [1...arr.length]
            nn = arr[i]
            if nn.text then n.text += '\n> '+nn.text
          @app.manager.save 'notes', n, (err) =>
            if err then return @app.showError err
            @reloadSheets()
      div = $(document.createElement('div')).addClass('note').appendTo(parent)
      div.attr draggable: yes, id: 'note'+note.id
      div.bind 'click', (e) =>
        e.preventDefault()
        if e.ctrlKey
          if @selectedNotes[note.id]
            delete @selectedNotes[note.id]
            div.removeClass 'note-selected'
          else
            @selectedNotes[note.id] = yes
            div.addClass 'note-selected'
        else
          @resetNoteSelection()
      div.bind 'dragstart', (e) =>
        offset = @app.dragGetOffset e, div
        if @selectedNotes[note.id]
          ids = []
          for id of @selectedNotes
            ids.push id
          @app.dragSetType e, 'custom/note', {ids: ids, x: offset.left, y: offset.top, append: e.shiftKey}
        else
          if e.ctrlKey
            createSpots(note)
          @app.dragSetType e, 'custom/note', {id: note.id, x: offset.left, y: offset.top, link: e.ctrlKey, append: e.shiftKey}
      div.bind 'dragover', (e) =>
        if (@app.dragHasType e, 'custom/note') or (@app.dragHasType e, 'Files')
          e.preventDefault()
      div.bind 'drop', (e) =>
        noteData = @app.dragGetType e, 'custom/note'
        if noteData?.id and noteData?.link
          log 'Dropped note:', noteData
          if @createNoteLink note, noteData.id
            e.stopPropagation()
            return no
        if noteData?.append
          mergeNotes(if noteData.id then [noteData.id] else noteData.ids)
          e.stopPropagation()
          return no
        files = @app.dragGetType e, 'Files'
        if files and files.length > 0
          # Upload first file
          @app.manager.storage.uploadFile files[0], (err, name) =>
            if err then return @app.showError err
            if not note.files
              note.files = []
            note.files.push name
            @app.manager.save 'notes', note, (err) =>
              if err then return @app.showError err
              log 'Note saved with attachment'
              @reloadSheets()
      div.bind 'dblclick', (e) =>
        @showNotesDialog sheet, note, () =>
          @reloadSheets()
        e.preventDefault()
        e.stopPropagation()
      div.addClass('note-color0 note-color'+(note.color ? 0))
      notewidth = note.width ? @noteWidths[@noteDefaultWidth]
      width = @preciseEm notewidth
      x = @preciseEm(note.x ? 0)
      y = @preciseEm(note.y ? 0)
      div.css width: "#{width}em", left: "#{x}em", top: "#{y}em"
      files = note.files ? []
      if note.collapsed
        div.addClass('note-collapsed')
      renderIcon = (fi) =>
        ICON_SIZE = 12
        file = files[fi]
        # log 'renderIcon', file, fi
        attDiv = $(document.createElement('div')).addClass('note-file').appendTo(div)
        iconsize = @preciseEm ICON_SIZE
        iconsizeexpanded = notewidth-8
        attDiv.css width: "#{iconsize}em", height: "#{iconsize}em"
        if _.endsWith(file, '.jpg')
          # image
          attDiv.addClass 'note-file-image'
          img = $(document.createElement('img'))
          img.css visibility: 'hidden'
          img.bind 'load', () =>
            mul = img.width()/iconsizeexpanded
            expwidth = @preciseEm iconsizeexpanded
            expheight = @preciseEm(img.height() / mul)
            mul = Math.max(img.width(), img.height()) / ICON_SIZE
            iconwidth = iconsize
            # iconheight = @preciseEm(img.height()/mul)
            iconheight = iconwidth
            div.bind 'mouseover', () =>
              attDiv.css width: "#{expwidth}em", height: "#{expheight}em"
              attDiv.addClass 'note-file-image-hover'
            div.bind 'mouseout', () =>
              attDiv.css width: "#{iconwidth}em", height: "#{iconheight}em"
              attDiv.removeClass 'note-file-image-hover'
            attDiv.css width: "#{iconwidth}em", height: "#{iconheight}em"
            img.addClass('note-image')
            img.css visibility: 'visible'
          @app.manager.storage.getFile file, (err, link) =>
            if not err
              img.attr 'src', link
              img.appendTo attDiv
        else
          attDiv.addClass 'note-file-other'
          span = $(document.createElement('span')).addClass('note-file-other-text').appendTo(attDiv)
          span.text(file.substr(file.lastIndexOf('.')))
        attDiv.bind 'dblclick', (e) =>
          e.stopPropagation()
          e.preventDefault()
          @app.showPrompt 'Are you sure want to delete file '+file+'?', () =>
            @app.manager.storage.removeFile file, (err) =>
              if err then return @app.showError err
              note.files.splice fi, 1
              @app.manager.save 'notes', note, (err) =>
                if err then return @app.showError err
                @reloadSheets()
          return no
      for fi in [0...files.length]
        renderIcon fi
      lines = (note.text ? '').split '\n'
      for i in [0...lines.length]
        line = lines[i]
        if not line
          if i<lines.length-1
            $(document.createElement('div')).addClass('note-br').html('&nbsp;').appendTo div
        else
          $(document.createElement('div')).addClass('note-line').appendTo(div).text line
      resizer = $(document.createElement('div')).addClass('note-resizer').appendTo(div)
      resizer.attr draggable: yes
      resizer.bind 'dragstart', (e) =>
        @app.dragSetType e, 'custom/note-resize', {id: note.id}
        e.stopPropagation()

    # , 'archived', {op: '<>', var: 1}
    renderBookmarks = (divItem) =>
      arr = @app.bookmarks[sheet.id]
      if not arr then return
      bmarkx = 0.5
      bmarky = 0
      bmarkstep = 2.5
      for item in arr
        divBMark = @renderBookmark item, 0.6, (bmark) =>
          @showBMarkDialog bmark, () =>
            @reloadSheetsBookmarks()
        divBMark.css top: "#{bmarky}em", right: "#{bmarkx}em"
        divItem.append divBMark
        bmarkx += bmarkstep
    @app.manager.storage.select 'notes', ['sheet_id', sheet.id], (err, arr) =>
      if err then return @app.showError err
      arr = arr.sort (a, b) ->
        if a.y < b.y then return -1
        if a.y > b.y then return 1
        if a.x < b.x then return -1
        if a.x > b.x then return 1
        return 0
      parent.children('div').remove()
      for item in arr
        loadNote item
        @notes.push item
      renderLinks(arr)
      renderBookmarks(parent)
  noteWidths: [50, 75, 90, 120, 150]
  noteDefaultWidth: 1
  zoomFactor: 5
  colors: 8
  gridStep: 3
  stick: yes

  loadSheet: (index, div) ->
    clearSelector = () =>
      if @selectorDiv
        @selectorDiv.remove()
        @selectorDiv = null
    clearSelector()
    inRectangle = (x1, y1, x2, y2, x3, y3, x4, y4) ->
      x_overlap = Math.min(x2,x4) - Math.max(x1,x3)
      y_overlap = Math.min(y2,y4) - Math.max(y1,y3)
      return if x_overlap>=0 and y_overlap>=0 then yes else no
    notesInRectangle = (x1, y1, x2, y2) =>
      if x1>x2 then [x1, x2] = [x2, x1]
      if y1>y2 then [y1, y2] = [y2, y1]
      notes = []
      for note in (@notes ? [])
        noteDiv = divContent.children('#note'+note.id)
        if noteDiv.size() isnt 1 then continue
        [width, height] = offsetToCoordinates(noteDiv.outerWidth(), noteDiv.outerHeight())
        if inRectangle(x1, y1, x2, y2, note.x, note.y, note.x+width, note.y+height)
          notes.push note
      [{x: x1, y: y1, width: x2-x1, height: y2-y1}, notes]

    offsetToCoordinates = (x, y) ->
      [Math.floor(x/divContent.width()*sheetWidth), Math.floor(y/divContent.height()*sheetHeight)]
    stickToGrid = (x, y) =>
      if @stick
        [Math.round(x/@gridStep)*@gridStep, Math.round(y/@gridStep)*@gridStep]
      else
        [x, y]
    sheet = @sheets[index]
    if not sheet then return null
    template = @app.getTemplate sheet.template_id
    templateConfig = @app.getTemplateConfig template
    sheetWidth = sheet?.config?.width ? template.width
    sheetHeight = sheet?.config?.height ? template.height
    divContent = div.find('.sheet_content')
    canvas = canto(div.find('.sheet-canvas').get(0))
    canvas.reset()
    width = @preciseEm sheetWidth
    height = @preciseEm sheetHeight
    divContent.css width: "#{width}em", height: "#{height}em"
    divTitle = div.find('.sheet_title')
    divTitle.text(sheet.title ? 'Untitled')
    div.find('.sheet-toolbar-custom').empty()
    plugins = sheet.plugins ? []
    for key in plugins
      if @app.sheetPlugins[key] then @app.sheetPlugins[key].load(div, sheet, this)
    div.find('.sheet-toolbar-edit').unbind('click').bind 'click', ()=>
      @showSheetDialog sheet, =>
        @reloadSheets()
    div.find('.sheet-toolbar-add-bmark').unbind('click').bind 'click', ()=>
      @showBMarkDialog {sheet_id: sheet.id, notepad_id: @notepad.id}, =>
        @reloadSheetsBookmarks()
    configButton = div.find('.sheet-toolbar-config')
    if templateConfig
      configButton.removeClass('disabled').unbind('click').bind 'click', ()=>
        templateConfig.configure template, sheet, this
    else
      configButton.addClass('disabled')
    divContent.unbind()
    divContent.bind 'mousedown', (e) =>
      offset = @app.dragGetOffset e, divContent
      [x, y] = offsetToCoordinates offset.left, offset.top
      [coords, notes] = notesInRectangle(x, y, x, y)
      # log 'mousedown', coords, notes
      if notes.length is 0 and e.ctrlKey
        @selectorDiv = $(document.createElement('div')).appendTo(divContent).addClass('notes-selector')
        @selectorDiv.css left: ''+@preciseEm(x)+'em', top: ''+@preciseEm(y)+'em'
        @selectorX = x
        @selectorY = y
        @selectorIndex = index
        return no
    divContent.bind 'mouseup', (e) =>
      if not e.ctrlKey and @selectorDiv
        clearSelector()
        return
      if @selectorDiv and @selectorIndex is index
        offset = @app.dragGetOffset e, divContent
        [x, y] = offsetToCoordinates offset.left, offset.top
        [coords, notes] = notesInRectangle(x, y, @selectorX, @selectorY)
        for note in notes
          if @selectedNotes[note.id]
            delete @selectedNotes[note.id]
            divContent.children('#note'+note.id).removeClass('note-selected')
          else
            @selectedNotes[note.id] = yes
            divContent.children('#note'+note.id).addClass('note-selected')
        clearSelector()

    divContent.bind 'mousemove', (e) =>
      if not e.ctrlKey and @selectorDiv
        clearSelector()
        return
      if @selectorDiv and @selectorIndex is index
        offset = @app.dragGetOffset e, divContent
        [x, y] = offsetToCoordinates offset.left, offset.top
        # log 'Before notes', x, y, @selectorX, @selectorY, e
        [coords, notes] = notesInRectangle(x, y, @selectorX, @selectorY)
        # log 'notesInRectangle', coords, notes
        @selectorDiv.css left: ''+@preciseEm(coords.x)+'em', top: ''+@preciseEm(coords.y)+'em', width: ''+@preciseEm(coords.width)+'em', height: ''+@preciseEm(coords.height)+'em'
    divContent.bind 'dblclick', (e) =>
      [x, y] = offsetToCoordinates e.offsetX, e.offsetY
      [x, y] = stickToGrid x, y
      @showNotesDialog sheet, {x: x, y: y}, () =>
        @reloadSheets()
      e.preventDefault()
    divContent.bind 'dragover', (e) =>
      #'custom/note-resize'
      if @app.dragHasType e, 'custom/note-resize'
        e.preventDefault()
      if @app.dragHasType e, 'custom/note'
        e.preventDefault()
      if @app.dragHasType e, 'custom/bmark'
        e.preventDefault()
      return false
    divContent.bind 'drop', (e) =>
      bmark = @app.dragGetType e, 'custom/bmark'
      if bmark
        @moveBookmark bmark.id, sheet
        e.stopPropagation()
        return no
      resizer = @app.dragGetType e, 'custom/note-resize'
      if resizer
        offset = @app.dragGetOffset e, divContent
        [x, y] = offsetToCoordinates offset.left, offset.top
        @app.manager.findOne 'notes', resizer.id, (err, note) =>
          if err then return @app.showError err
          width = Math.max(@noteWidths[0], x-note.x)
          newWidth = width
          for w in @noteWidths
            if w<=width
              newWidth = w
          note.width = newWidth
          @app.manager.save 'notes', note, (err) =>
            if err then return @app.showError err
            @reloadSheets()
        e.stopPropagation()
        return no
      if @app.dragHasType e, 'custom/note'
        otherNote = @app.dragGetType e, 'custom/note'
        offset = @app.dragGetOffset e, divContent
        [x, y] = offsetToCoordinates offset.left-otherNote.x, offset.top-otherNote.y
        [x, y] = stickToGrid x, y
        if otherNote.id
          @app.manager.findOne 'notes', otherNote?.id, (err, note) =>
            if err then return @app.showError err
            note.x = x
            note.y = y
            note.sheet_id = sheet.id
            @app.manager.storage.update 'notes', note, (err) =>
              if err then return @app.showError err
              @reloadSheets()
        else
          config = []
          for id in otherNote.ids
            config.push type: 'findOne', id: id, stream: 'notes'
          @app.manager.batch config, (err, arr) =>
            if err then return @app.showError err
            updates = []
            moveX = 0
            moveY = 0
            arr = arr.sort (a, b) ->
              if a.y<b.y then return -1
              if a.y>b.y then return 1
              return a.x-b.x
            for index in [0...arr.length]
              note = arr[index]
              if index is 0
                # Calc move
                moveX = note.x-x
                moveY = note.y-y
              note.sheet_id = sheet.id
              note.x -= moveX
              note.y -= moveY
              updates.push type: 'update', stream: 'notes', object: note
            @app.manager.batch updates, (err) =>
              if err then return @app.showError err
              @reloadSheets()
      return false
    # log 'Before render:', divContent.width(), divContent.height()
    canvas.width = divContent.width()
    canvas.height = divContent.height()
    zoom = divContent.width()/sheetWidth
    #if templateConfig
    bounds = {width: sheetWidth, height: sheetHeight}
    @app.templateDrawer.render template, sheet, canvas, zoom, bounds
    for key in plugins
      if @app.sheetPlugins[key]
        conf = @app.sheetPlugins[key].getConfig(sheet.config)
        log 'Draw:', sheet.config, conf, key
        if conf?.draw then @app.templateDrawer.draw(conf.draw, canvas, zoom, bounds)
    @loadNotes sheet, divContent, canvas, zoom
    return template

class TemplateManager

  constructor: (@app) ->
    $('.templates-add').bind 'click', () =>
      @edit()
      return no
    $('.template-save').bind 'click', () =>
      @save()
      return no
    $('.template-remove').bind 'click', () =>
      @remove()
      return no
    $('.template-clone').bind 'click', () =>
      @clone()
      return no
    @editName = $('#template-name')
    @editTag = $('#template-tag')
    @editWidth = $('#template-width')
    @editHeight = $('#template-height')
    @editType = $('#template-type')
    @editConfig = $('#template-config')
    @disableForm()

  disableForm: ->
    form = $('.template-form')
    form.find('input, button, textarea').attr disabled: 'disabled'
    form.find('input, textarea').val ''

  enableForm: ->
    form = $('.template-form')
    form.find('input, textarea').attr disabled: null

  clone: ->
    delete @selected.id
    @selected.name = 'Copy of '+@selected.name
    @app.manager.save 'templates', @selected, (err) =>
      if err then return @app.showError err
      @refresh()
      @edit @selected
      @app.refreshTemplates()

  remove: ->
    @app.showPrompt 'Are you sure want to remove template? It\'ll reset template of associated sheets', =>
      @app.manager.storage.select 'sheets', ['template_id', @selected.id], (err, data) =>
        if err then return @app.showError err
        config = []
        for sheet in data
          delete sheet.template_id
          config.push type: 'update', stream: 'sheets', object: sheet
        config.push type: 'removeCascade', stream: 'templates', object: @selected
        @app.manager.batch config, (err) =>
          if err then return @app.showError err
          @selected = null
          @refresh()
          @app.refreshTemplates()
  save: ->
    name = @editName.val().trim()
    if not name
      return @app.showError 'Name is empty'
    config = {}
    try
      config = JSON.parse @editConfig.val()
    catch e
      return @app.showError 'Config is not JSON'
    @selected.name = name
    @selected.tag = @editTag.val().trim()
    @selected.width = parseInt(@editWidth.val()) ? @selected.width
    @selected.height = parseInt(@editHeight.val()) ? @selected.height
    @selected.type = @editType.val().trim()
    @selected.config = config
    @app.manager.save 'templates', @selected, (err) =>
      if err then return @app.showError err
      @refresh()
      @edit @selected
      @app.refreshTemplates()

  edit: (template = {width: 102, height: 144}) ->
    @enableForm()
    log 'edit', template
    $('.template-save').attr disabled: null
    if template.id
      #edit
      $('.template-remove').attr disabled: null
      $('.template-clone').attr disabled: null
    else
      $('.template-remove').attr disabled: 'disabled'
      $('.template-clone').attr disabled: 'disabled'
    @selected = template
    @editName.val(template.name ? 'Untitled').focus().select()
    @editTag.val(template.tag ? '')
    @editWidth.val(template.width ? 0)
    @editHeight.val(template.height ? 0)
    @editType.val(template.type ? '')
    @editConfig.val(if template.config then JSON.stringify(template.config, null, 2) else '{}')

  refresh: (selectID) ->
    ul = $('.templates-list-ul')
    @app.manager.storage.select 'templates', [], (err, data) =>
      if err then return @app.showError err
      ul.empty()
      found = no
      data = @app.sortTemplates data
      for section in data
        li = $(document.createElement('li')).addClass('nav-header')
        li.text(section.title)
        li.appendTo ul
        for item in section.items
          li = $(document.createElement('li'))
          if @selected?.id is item.id
            found = yes
            li.addClass('active')
          a = $(document.createElement('a')).attr 'href': '#'
          a.text(item.name)
          a.appendTo li
          li.appendTo ul
          do (item, li) =>
            a.bind 'click', (e) =>
              @edit item
              @refresh()
              return no
      if not found then @disableForm()
    , order: ['tag', 'name']

class TemplateConfig

  constructor: (@app) ->

  configure: (tmpl, sheet, controller) ->

  preciseCoord: (value) ->
    return Math.round(value*10)/10

class SheetPlugin

  constructor: (@app, @key) ->

  load: (div, sheet, controller) ->

  getConfig: (config) ->
    conf = config?["_#{@key}"] ? {}
    return conf

  setConfig: (config, conf) ->
    config["_#{@key}"] = conf

  addButton: (div, cls, handler) ->
    parent = div.find('.sheet-toolbar-custom')
    a = $(document.createElement('a')).addClass('btn').appendTo(parent)
    a.attr(href: '#')
    i = $(document.createElement('i')).addClass(cls).appendTo(a)
    a.bind 'click', (e) =>
      handler()
      return no

window.TemplateConfig = TemplateConfig
window.SheetPlugin = SheetPlugin