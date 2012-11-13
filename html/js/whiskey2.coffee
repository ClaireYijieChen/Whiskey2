yepnope({
  load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/whiskey2.css', 'lib/lima1/net.js', 'lib/lima1/main.js'],
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
    @oauth.token = @manager.get('token', 'no-value')
    @manager.open (error) =>
      if error
        @manager = null
        @showError error
      @showAlert 'Application started', severity: 'success'
      @oauth.on_token_error = =>
        @showLoginDialog()
      @bindMain()
      @refreshNotepads()

  showLoginDialog: ->
    $('#main-password').val('')
    $('#main-login-dialog').modal('show')

  bindMain: ->
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

  dragSetType: (e, type, value) ->
    if e?.originalEvent?.dataTransfer?.setData
      e.originalEvent.dataTransfer.setData type, JSON.stringify(value)

  dragGetType: (e, type) ->
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

  refreshNotepads: (selectID) ->
    $('#main-tabs li.main-tab-notepad').remove()
    $('#main-tabs-content .main-tab-notepad').remove()
    @manager.storage.select 'notepads', ['archived', {op: '<>', var: 1}], (err, arr) =>
      if err then return @showError err
      # log 'Notepads:', arr
      for item in arr
        log 'Item', item
        li = $(document.createElement('li')).addClass('main-tab-notepad')
        a = $(document.createElement('a')).attr(href: '#np'+item.id).text(' '+item.name).appendTo li
        a.prepend('<i class="icon-book"></i>')
        $('#main-tabs').prepend li
        div = $(document.createElement('div')).addClass('tab-pane main-tab-notepad').attr(id: 'np'+item.id)
        $('#main-tabs-content').append(div)
        do (a) =>
          a.bind 'dragover', (e) =>
            if @dragHasType e, 'custom/note'
              e.preventDefault()
          a.bind 'dragenter', (e) =>
            if @dragHasType e, 'custom/note'
              a.tab('show')
              e.preventDefault()
          a.bind 'click', (e) =>
            e.preventDefault()
            a.tab('show')
        @renderNotepad div, item
        if item.id is selectID
          a.tab('show')

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
    return new Notepad @, notepad, div

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

  sync: ->
    if not @manager then return
    # @showAlert 'Sync started'
    @manager.sync (err) =>
      if err then return @showError err
      # @showAlert 'Sync done'
    , no, (type) =>
      w = 100
      switch type
        when 0 then w = 25
        when 1 then w = 50
        when 2 then w = 75
      log 'Sync progress', w, type
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
    width: 100
    height: 141
  }
  getTemplate: (id) ->
    return @emptyTemplate

  findByID: (arr, id, start = 0) ->
    if not id then return -1
    for i in [start...arr.length]
      if arr[i].id is id then return i
    return -1

  removeWithNext: (sink, array, items) ->
    # Removes items and adds updated elements to array
    for item in items
      index = @findByID array, item.id
      if index is -1 then continue
      # item at the left will refer to item at the right
      if index>0
        if index<array.length-1
          # link
          array[index-1].next_id = array[index+1].id
        else
          # null
          array[index-1].next_id = null
        if @findByID sink, array[index-1].id is -1 then sink.add array[index-1]

  addWithNext: (sink, array, before, items) ->
    # Adds items to array and updates next_id. if before is -1 - add to the end
    if not items?.length then return
    if not array?.length
      next_id = null
      for item in items
        item.next_id = next_id
        next_id = item.id
      return
    next_id = null

  sortWithNext: (items) ->
    if not items?.length then return items
    i = 0
    while i<items.length
      item = items[i]
      # Have next_id
      index = @findByID items, item.next_id
      if index isnt -1
        # found
        found = items[index]
        if index<i
          # left
          items.splice i, 1
          items.splice index, 0, found
        else
          if index>i+1
            # right (not same and not next)
            items.splice index, 1
            items.splice i+1, 0, found
      i++ # next item
    return items

class Notepad

  constructor: (@app, @notepad, @div) ->
    @div.find('.notepad-name').text(@notepad.name)
    @divMiniatures = div.find('.notepad-sheets')
    @divContent = div.find('.notepad-content')
    @div.find('.notepad-add-sheet').bind 'click', () =>
      @showSheetDialog {}, =>
        @reloadSheets()
    @div.find('.notepad-reload-sheets').bind 'click', =>
      @reloadSheets()
    @reloadSheets()

  showSheetDialog: (sheet, handler) ->
    $('#sheet-dialog-name').val(sheet.title ? '')
    $('#sheet-dialog').modal('show')
    $('#do-save-sheet-dialog').unbind('click').bind 'click', (e) =>
      name = $('#sheet-dialog-name').val().trim()
      if not name then return @app.showError 'Title is empty'
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

  renderSheetMiniatures: (sheets) ->
    div = @divMiniatures
    div.empty()
    if not sheets?.length then return
    minHeight = 3
    maxHeight = 30
    oneItemHeight = 50
    height = $(window).height() - 150
    step = maxHeight
    if sheets.length>1
      step = Math.floor((height-oneItemHeight)/(sheets.length-1))
    if step>maxHeight then step = maxHeight
    if step<minHeight then step = minHeight
    h = 0
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
        divItem.bind 'click', (e) =>
          e.preventDefault()
          @loadSheets i
        divItem.bind 'dragstart', (e) =>
          @app.dragSetType e, 'custom/sheet', sheet
        divItem.bind 'dragover', (e) =>
          if @app.dragHasType e, 'custom/sheet'
            e.preventDefault()
          return no
        divItem.bind 'drop', (e) =>
          otherSheet = @app.dragGetType e, 'custom/sheet'
          index = @app.findByID sheets, otherSheet?.id
          log 'Dropped sheet', otherSheet, index, i
          if otherSheet and index isnt -1
            e.stopPropagation()
          return no
        divItem.bind 'mouseover', (e) =>
          divItem.addClass('notepad-sheet-miniature-hover')
        divItem.bind 'dragenter', (e) =>
          divItem.addClass('notepad-sheet-miniature-hover')
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
      if @lastSheetID
        index = @app.findByID @sheets, @lastSheetID
        if index isnt -1 then @loadSheets index

  maxSheetsVisible: 2
  loadSheets: (index) ->
    @divContent.empty()
    width = @divContent.innerWidth()
    totalWidth = 0
    @lastSheetID = @sheets[index]?.id
    count = 0
    while totalWidth<width and count<@maxSheetsVisible
      div = @loadSheet(index)
      if not div then break
      totalWidth += div.outerWidth()
      index++
      count++

  showNotesDialog: (sheet, note, handler) ->
    $('#note-dialog').modal('show')
    $('#note-dialog-text').val(note.text ? '').focus()
    $('#note-dialog-collapsed').attr 'checked': if note.collapsed then yes else no
    colors = $('#note-dialog-colors').empty()
    widths = $('#note-dialog-widths').empty()
    for color in [0...@colors]
      a = $(document.createElement('a')).addClass('btn').attr(href: '#').data('index', color)
      a.appendTo colors
      if (note.color ? 0) is color
        a.addClass 'active'
      adiv = $(document.createElement('div')).addClass('note-color-button note-color'+color).html('&nbsp;').appendTo(a)
    colors.button()
    for width in @noteWidths
      a = $(document.createElement('a')).addClass('btn').attr(href: '#').data('width', width)
      a.text("#{width}%").appendTo widths
      if (note.width ? @noteWidths[0]) is width
        a.addClass 'active'
    widths.button()
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
        note.sheet_id = sheet.id
        @app.manager.storage.create 'notes', note, _handler
      else
        @app.manager.storage.update 'notes', note, _handler

  loadNotes: (sheet, parent) ->
    preciseEm = (value) =>
      return Math.floor(value*10/@zoomFactor)/10
    loadNote = (note) =>
      # log 'loadNote', note
      div = $(document.createElement('div')).addClass('note').appendTo(parent)
      div.attr draggable: yes
      div.bind 'dragstart', (e) =>
        offset = @app.dragGetOffset e, div
        @app.dragSetType e, 'custom/note', {id: note.id, x: offset.left, y: offset.top}
      div.bind 'dblclick', (e) =>
        @showNotesDialog sheet, note, () =>
          @reloadSheets()
        e.preventDefault()
        e.stopPropagation()
      div.addClass('note-color0 note-color'+(note.color ? 0))
      width = note.width ? @noteWidths[0]
      width = preciseEm width
      x = preciseEm(note.x ? 0)
      y = preciseEm(note.y ? 0)
      div.css width: "#{width}em", left: "#{x}em", top: "#{y}em"
      if note.collapsed
        div.addClass('note-collapsed')
      lines = (note.text ? '').split '\n'
      for i in [0...lines.length]
        line = lines[i]
        if not line
          if i<lines.length-1
            $(document.createElement('div')).addClass('note-br').html('&nbsp;').appendTo div
        else
          $(document.createElement('div')).addClass('note-line').appendTo(div).text line
    parent.empty()
    # , 'archived', {op: '<>', var: 1}
    @app.manager.storage.select 'notes', ['sheet_id', sheet.id], (err, arr) =>
      if err then return @app.showError err
      for item in arr
        loadNote item
  noteWidths: [50, 75, 90, 125]
  zoomFactor: 5
  colors: 5

  loadSheet: (index) ->
    offsetToCoordinates = (x, y) ->
      [Math.floor(x/divContent.width()*template.width), Math.floor(y/divContent.height()*template.height)]
    sheet = @sheets[index]
    if not sheet then return null
    template = @app.getTemplate sheet.template_id
    div = $(document.createElement('div')).addClass('sheet').appendTo @divContent
    divContent = $(document.createElement('div')).addClass('sheet_content').appendTo div
    width = Math.floor(template.width/@zoomFactor)
    height = Math.floor(template.height/@zoomFactor)
    divContent.css width: "#{width}em", height: "#{height}em"
    divTitle = $(document.createElement('div')).addClass('sheet_title').appendTo div
    divTitle.text(sheet.title ? 'Untitled')
    divToolbar = $('#sheet-toolbar-template').clone().removeClass('hide').appendTo div
    divToolbar.find('.sheet-toolbar-edit').bind 'click', ()=>
      @showSheetDialog sheet, =>
        @reloadSheets()
    divContent.bind 'dblclick', (e) =>
      [x, y] = offsetToCoordinates e.offsetX, e.offsetY
      @showNotesDialog sheet, {x: x, y: y}, () =>
        @reloadSheets()
      e.preventDefault()
    divContent.bind 'dragover', (e) =>
      if @app.dragHasType e, 'custom/note'
        e.preventDefault()
      return false
    divContent.bind 'drop', (e) =>
      if @app.dragHasType e, 'custom/note'
        otherNote = @app.dragGetType e, 'custom/note'
        @app.manager.findOne 'notes', otherNote?.id, (err, note) =>
          if err then return @app.showError err
          offset = @app.dragGetOffset e, divContent
          [x, y] = offsetToCoordinates offset.left-otherNote.x, offset.top-otherNote.y
          note.x = x
          note.y = y
          note.sheet_id = sheet.id
          @app.manager.storage.update 'notes', note, (err) =>
            if err then return @app.showError err
            @reloadSheets()
      return false
    @loadNotes sheet, divContent
    return div