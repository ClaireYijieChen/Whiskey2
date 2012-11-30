yepnope({
  load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/whiskey2.css', 'lib/lima1/net.js', 'lib/lima1/main.js', 'bs/js/bootstrap-colorpicker.js', 'bs/js/bootstrap-datepicker.js', 'bs/css/datepicker.css', 'bs/css/colorpicker.css'],
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
    @manager.open (error) =>
      if error
        @manager = null
        @showError error
      @showAlert 'Application started', severity: 'success'
      @oauth.on_token_error = =>
        @showLoginDialog()
      @bindMain()
      @refreshBookmarks () =>
        @refreshNotepads()
      @sync()

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
      @sortWithNext arr
      moveNotepad = (index, id) =>
        @moveWithNext 'notepads', arr, index, [id], (err) =>
          if err then return @showError err
          @refreshNotepads selectID
      for i in [0...arr.length]
        item = arr[i] # Revert
        li = $(document.createElement('li')).addClass('main-tab-notepad')
        a = $(document.createElement('a')).attr(href: '#np'+item.id).text(' '+item.name).appendTo li
        a.prepend('<i class="icon-book"></i>')
        li. insertBefore $('#main-tab-templates')
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

  sync: ->
    if not @manager then return
    # @showAlert 'Sync started'
    @manager.sync (err) =>
      if err then return @showError err
      # @showAlert 'Sync done'
    , (type) =>
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
    $('#sheet-dialog-name').val(sheet.title ? '')
    $('#sheet-dialog').modal('show')
    $('#do-remove-sheet-dialog').unbind('click').bind 'click', (e) =>
      if not sheet.id then return
      @app.showPrompt 'Are you sure want to remove sheet? It will remove notes also', =>
        @removeSheets [sheet.id]
      $('#sheet-dialog').modal('hide')
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
    height = $(window).height() - 150
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
      if @lastSheetID
        index = @app.findByID @sheets, @lastSheetID
        if index isnt -1 then @loadSheets index

  maxSheetsVisible: 2

  initSheetPlaceholders: () ->
    @sheetDivs = []
    for i in [0...@maxSheetsVisible]
      div = $(document.createElement('div')).addClass('sheet').appendTo @divContent
      divContent = $(document.createElement('div')).addClass('sheet_content').appendTo div
      divTitle = $(document.createElement('div')).addClass('sheet_title').appendTo div
      divToolbar = $('#sheet-toolbar-template').clone().removeClass('hide').appendTo div
      @sheetDivs.push div

  loadSheets: (index) ->
    @selectedNotes = {}
    @lastSheetID = @sheets[index]?.id
    count = 0
    @notes = []
    while count<@maxSheetsVisible
      div = @sheetDivs[count]
      if not @sheets[index]
        div.hide()
      else
        div.show()
        @loadSheet(index, div)
      index++
      count++

  showNotesDialog: (sheet, note, handler) ->
    $('#note-dialog').modal('show')
    $('#note-dialog-text').val(note.text ? '').focus()
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

  loadNotes: (sheet, parent) ->
    loadNote = (note) =>
      # log 'loadNote', note
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
          @app.dragSetType e, 'custom/note', {ids: ids, x: offset.left, y: offset.top}
        else
          @app.dragSetType e, 'custom/note', {id: note.id, x: offset.left, y: offset.top}
      div.bind 'dblclick', (e) =>
        @showNotesDialog sheet, note, () =>
          @reloadSheets()
        e.preventDefault()
        e.stopPropagation()
      div.addClass('note-color0 note-color'+(note.color ? 0))
      width = note.width ? @noteWidths[@noteDefaultWidth]
      width = @preciseEm width
      x = @preciseEm(note.x ? 0)
      y = @preciseEm(note.y ? 0)
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
      parent.empty()
      for item in arr
        loadNote item
        @notes.push item
      renderBookmarks(parent)
  noteWidths: [50, 75, 90, 125]
  noteDefaultWidth: 1
  zoomFactor: 5
  colors: 8
  gridStep: 6
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
      [Math.floor(x/divContent.width()*template.width), Math.floor(y/divContent.height()*template.height)]
    stickToGrid = (x, y) =>
      if @stick
        [Math.round(x/@gridStep)*@gridStep, Math.round(y/@gridStep)*@gridStep]
      else
        [x, y]
    sheet = @sheets[index]
    if not sheet then return null
    template = @app.getTemplate sheet.template_id
    divContent = div.find('.sheet_content')
    width = Math.floor(template.width/@zoomFactor)
    height = Math.floor(template.height/@zoomFactor)
    divContent.css width: "#{width}em", height: "#{height}em"
    divTitle = div.find('.sheet_title')
    divTitle.text(sheet.title ? 'Untitled')
    div.find('.sheet-toolbar-edit').unbind('click').bind 'click', ()=>
      @showSheetDialog sheet, =>
        @reloadSheets()
    div.find('.sheet-toolbar-add-bmark').unbind('click').bind 'click', ()=>
      @showBMarkDialog {sheet_id: sheet.id}, =>
        @reloadSheetsBookmarks()
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
            arr = arr.sort (a, b) ->
              if a.y<b.y then return -1
              if a.y>b.y then return 1
              return a.x-b.x
            for note in arr
              note.sheet_id = sheet.id
              note.x = x
              note.y = y
              y+= @gridStep*2
              updates.push type: 'update', stream: 'notes', object: note
            @app.manager.batch updates, (err) =>
              if err then return @app.showError err
              @reloadSheets()
      return false
    @loadNotes sheet, divContent
    return div
