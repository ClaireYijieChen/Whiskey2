class DialogTemplateConfig extends TemplateConfig
  title: 'Sheet config'
  tmpl: """
  <div class=\"modal hide\">
    <div class=\"modal-header\">
      <h3>Grid sheet config</h3>
    </div>
    <div class=\"modal-body\">
    </div>
    <div class=\"modal-footer\">
        <a href=\"#\" class=\"btn btn-primary dialog-do-save\">Save</a>
        <a href=\"#\" class=\"btn dialog-do-close\">Close</a>
    </div>
  </div>
  """
  sheetSizeTmpl: """
  <form class=\"form-horizontal\">
    <div class=\"control-group\">
      <label class=\"control-label\" for=\"main-login\">Page size:</label>
      <div class=\"controls\">
        <select class=\"size-select\">
        </select>
      </div>
    </div>
    <div class=\"control-group\">
      <label class=\"control-label\" for=\"main-password\">Orientation:</label>
      <div class=\"controls\">
        <select class=\"orientation-select\">
          <option value=\"0\">Portrait</option>
          <option value=\"1\">Landscape</option>
        </select>
      </div>
    </div>
  </form>
  """
  sizes: [{caption: 'A6', width: 102, height: 144}, {caption: 'A5', width: 144, height: 204}, {caption: 'A4', width: 204, height: 288}, {caption: 'A3', width: 288, height: 408}]

  createForm: (div, tmpl, config) ->
    return () ->

  createSheetSizeForm: (div, tmpl, config) ->
    form = $(@sheetSizeTmpl).appendTo(div)
    config.size = config.size ? 0
    config.orientation = config.orientation ? 0
    sizeSelect = form.find('.size-select')
    for i in [0...@sizes.length]
      size = @sizes[i]
      option = $(document.createElement('option')).appendTo(sizeSelect)
      option.attr(value: i)
      option.text(size.caption)
    sizeSelect.val(config.size)
    orientationSelect = form.find('.orientation-select')
    orientationSelect.val(config.orientation)
    return () =>
      config.size = parseInt(sizeSelect.val())
      config.orientation = parseInt(orientationSelect.val())
      size = @sizes[config.size]
      if size
        config.width = size.width
        config.height = size.height
        if config.orientation is 1
          [config.width, config.height] = [config.height, config.width]


  configure: (tmpl, sheet, controller) ->
    div = $(@tmpl).appendTo(document.body)
    div.find('.modal-header h3').text(@title)
    div.modal backdrop: 'static', keyboard: no
    div.find('.dialog-do-close').bind 'click', () =>
      div.modal('hide').remove()
    config = sheet.config ? {}
    fn = @createForm(div.find('.modal-body'), tmpl, config)
    div.find('.dialog-do-save').bind 'click', () =>
      fn()
      sheet.config = config
      @app.manager.save 'sheets', sheet, (err) =>
        if err then return @app.showError err
        controller.reloadSheets()
        div.modal('hide').remove()

class ChronodexConfig extends DialogTemplateConfig
  title: 'Chronodex config'

  formTmpl: """
  <form class=\"form-horizontal\">
    <div class=\"control-group\">
      <label class=\"control-label\">Position:</label>
      <div class=\"controls\">
        <select class=\"align-select\">
          <option value=\"l\">Left</option>
          <option value=\"c\">Center</option>
          <option value=\"r\">Right</option>
        </select>
        <select class=\"valign-select\">
          <option value=\"t\">Top</option>
          <option value=\"m\">Middle</option>
          <option value=\"b\">Bottom</option>
        </select>
      </div>
    </div>
    <div class=\"control-group\">
      <label class=\"control-label\">Zoom (%):</label>
      <div class=\"controls\">
        <input type=\"text\" class=\"zoom-text\"/>
      </div>
    </div>
  </form>
  """
  createForm: (div, tmpl, config) ->
    form = $(@formTmpl).appendTo(div)
    config.align = config.align ? 'c'
    config.valign = config.valign ? 'm'
    config.zoom = config.zoom ? 100
    alignSelect = form.find('.align-select').val(config.align)
    valignSelect = form.find('.valign-select').val(config.valign)
    zoomText = form.find('.zoom-text').val(config.zoom)
    sizeFn = @createSheetSizeForm(div, tmpl, config)
    return () =>
      config.align = alignSelect.val()
      config.valign = valignSelect.val()
      config.zoom = parseInt(zoomText.val()) ? 100
      sizeFn()
      config.draw = @generate(config)

  generate: (config) ->
    draw = []
    centerx = config.width / 2
    centery = config.height / 2
    circleRadius = 10
    smallCRadius = 2
    smallRadius = 15
    middleRadius = 20
    bigRadius = 25
    degStep = 30
    heights = [smallRadius, middleRadius, bigRadius]
    spotHeights = [bigRadius, middleRadius, bigRadius]
    lineColor = '#000000'
    lineGrayColor = '#aaaaaa'
    littleRadius = 6
    config.spots = []
    times = [
      {x: 0, y: littleRadius-1, t: '6am'}
      {x: -3, y: littleRadius-4, t: '7am'}
      {x: -littleRadius+1, y: -1, t: '8am'}
      {x: -smallRadius-6, y: -1, t: '9am'}
      {x: -middleRadius-6, y: -smallRadius+4, t: '10am'}
      {x: -middleRadius, y: -bigRadius+2, t: '11am'}
      {x: 0, y: -bigRadius-3, t: '12am'}
      {x: smallRadius-3, y: -middleRadius+2, t: '1pm'}
      {x: middleRadius+2, y: -smallRadius, t: '2pm'}
      {x: bigRadius+3, y: 0, t: '3pm'}
      {x: middleRadius-1, y: smallRadius-2, t: '4pm'}
      {x: smallRadius-2, y: bigRadius, t: '5pm'}
      {x: 0, y: bigRadius+5, t: '6pm'}
      {x: -smallRadius, y: middleRadius, t: '7pm'}
      {x: -bigRadius, y: smallRadius, t: '8pm'}
      {x: -bigRadius-8, y: 0, t: '9pm'}
    ]
    draw.push(type: 'circle', color: lineColor, x: bigRadius, y: 0, r: smallCRadius)
    draw.push(type: 'circle', color: lineColor, x: 0, y: bigRadius, r: smallCRadius)
    draw.push(type: 'circle', color: lineColor, x: -bigRadius, y: 0, r: smallCRadius)
    draw.push(type: 'circle', color: lineColor, x: 0, y: -bigRadius, r: smallCRadius)
    draw.push(type: 'circle', color: lineColor, x: 0, y: 0, r: circleRadius)
    draw.push(type: 'arc', color: lineGrayColor, x: 0, y: 0, r: littleRadius, sa: 90, ea: 180)
    pi180 = Math.PI/180
    zoom = config.zoom / 100
    xyWithAngle = (deg, radius) =>
      [@preciseCoord(centerx+Math.round(Math.cos(deg*pi180)*radius*zoom)), @preciseCoord(centery+Math.round(Math.sin(deg*pi180)*radius*zoom))]
    deg = 90
    for i in [0...4]
      itm = type: 'move', a: deg, items: [{type: 'line', x1: littleRadius, y1: 0, x2: circleRadius, y2: 0, color: lineGrayColor}]
      draw.push(itm)
      [x, y] = xyWithAngle(deg, circleRadius)
      config.spots.push(id: 'h'+(6+i), x: x, y: y)
      deg += degStep
    deg = 0
    for i in [0...12]
      height = heights[i % heights.length]
      [x, y] = xyWithAngle(deg, spotHeights[i % spotHeights.length])
      config.spots.push(id: 'h'+(if i>7 then i+2 else i+15), x: x, y: y)
      draw.push(type: 'arc', color: lineColor, x: 0, y: 0, r: height, sa: deg, ea: deg+degStep)
      draw.push(type: 'move', a: deg, items: [{type: 'line', x1: circleRadius, y1: 0, x2: height, y2: 0, color: lineColor}])
      draw.push(itm)
      deg += degStep
      draw.push(type: 'move', a: deg, items: [{type: 'line', x1: circleRadius, y1: 0, x2: height, y2: 0, color: lineColor}])
    for i, time of times
      draw.push(type: 'text', text: time.t, x: time.x ? 0, y: time.y ? 0, size: -3)
    return [{type: 'move', x: centerx, y: centery, items: draw, z: config.zoom}]

class GridTemplateConfig extends TemplateConfig

  tmpl: """
  <div class=\"modal hide\">
    <div class=\"modal-header\">
      <h3>Grid sheet config</h3>
    </div>
    <div class=\"modal-body\">
      <table class=\"grid-table\" style=\"width: 100%;\">
        <tbody></tbody>
      </table>
      <div>
        <a href=\"#\" class=\"btn btn-success dialog-do-add-row\">+ Row</a>
        <a href=\"#\" class=\"btn btn-success dialog-do-add-col\">+ Column</a>
        <a href=\"#\" class=\"btn btn-danger dialog-do-remove-row\">- Row</a>
        <a href=\"#\" class=\"btn btn-danger dialog-do-remove-col\">- Column</a>
      </div>
      <div style=\"margin-top: 0.5em;\">
        <select class=\"size-select\">
        </select>
        <select class=\"orientation-select\">
          <option value=\"0\">Portrait</option>
          <option value=\"1\">Landscape</option>
        </select>
      </div>
    </div>
    <div class=\"modal-footer\">
        <a href=\"#\" class=\"btn btn-primary dialog-do-save\">Save</a>
        <a href=\"#\" class=\"btn dialog-do-close\">Close</a>
    </div>
  </div>
  """
  sizes: [{caption: 'A6', width: 102, height: 144}, {caption: 'A5', width: 144, height: 204}, {caption: 'A4', width: 204, height: 288}, {caption: 'A3', width: 288, height: 408}]
  configure: (tmpl, sheet, controller) ->
    div = $(@tmpl).appendTo(document.body)
    div.modal backdrop: 'static', keyboard: no
    div.find('.dialog-do-close').bind 'click', () =>
      div.modal('hide').remove()
    config = sheet.config ? {}
    config.cols = config.cols ? 2
    config.rows = config.rows ? 2
    config.size = config.size ? 0
    config.orientation = config.orientation ? 0
    inputs = []
    saveTable = =>
      cols = config.cols
      rows = config.rows
      config.captions = []
      for j in [0...rows]
        row = inputs[j]
        captions = []
        config.captions.push(captions)
        for i in [0...cols]
          captions[i] = ''
          if row and row[i] then captions[i] = row[i].val().trim()
    refreshTable = () =>
      cols = config.cols
      rows = config.rows
      tbody = div.find('.grid-table tbody').empty()
      inputs = []
      for j in [0...rows]
        tr = $(document.createElement('tr')).appendTo(tbody)
        inputsRow = []
        inputs.push(inputsRow)
        captions = config.captions?[j]
        for i in [0...cols]
          width = Math.floor(100/(cols))
          td = $(document.createElement('td')).appendTo(tr)
          td.width("#{width}%")
          input = $(document.createElement('input')).attr(type: 'text').appendTo(td)
          input.css(width: '100%', 'margin-right': '4px', 'padding': '4px 0px', 'margin-top': '9px')
          if captions?[i] then input.val(captions?[i])
          inputsRow.push(input)
    sizeSelect = div.find('.size-select')
    for i in [0...@sizes.length]
      size = @sizes[i]
      option = $(document.createElement('option')).appendTo(sizeSelect)
      option.attr(value: i)
      option.text(size.caption)
    sizeSelect.val(config.size)
    orientationSelect = div.find('.orientation-select')
    orientationSelect.val(config.orientation)
    refreshTable()
    div.find('.dialog-do-add-row').bind 'click', () =>
      config.rows++
      saveTable()
      refreshTable()
    div.find('.dialog-do-add-col').bind 'click', () =>
      config.cols++
      saveTable()
      refreshTable()
    div.find('.dialog-do-remove-row').bind 'click', () =>
      if config.rows>1
        config.rows--
        saveTable()
        refreshTable()
    div.find('.dialog-do-remove-col').bind 'click', () =>
      if config.cols>1
        config.cols--
        saveTable()
        refreshTable()
    div.find('.dialog-do-save').bind 'click', () =>
      saveTable()
      config.size = parseInt(sizeSelect.val())
      config.orientation = parseInt(orientationSelect.val())
      size = @sizes[config.size]
      if size
        config.width = size.width
        config.height = size.height
        if config.orientation is 1
          [config.width, config.height] = [config.height, config.width]
      config.draw = @generate config
      sheet.config = config
      @app.manager.save 'sheets', sheet, (err) =>
        if err then return @app.showError err
        controller.reloadSheets()
        div.modal('hide').remove()

  LINE_PADDING: 3
  TEXT_PADDING_X: 3
  TEXT_PADDING_Y: 3
  LINE_COLOR: '#dddddd'
  TEXT_COLOR: '#aaaaaa'
  TEXT_SIZE: -1
  generate: (config) ->
    draw = []
    stepRows = (config.height-2*@LINE_PADDING) / config.rows
    stepCols = (config.width-2*@LINE_PADDING) / config.cols
    left = @LINE_PADDING
    for i in [0...config.cols]
      # Draw vertical lines
      x = Math.round(left)
      left += stepCols
      if i>0
        draw.push(type: 'line', x1: x, y1: @LINE_PADDING, x2: x, y2: config.height - @LINE_PADDING, color: @LINE_COLOR)
    top = @LINE_PADDING
    for j in [0...config.rows]
      y = Math.round(top)
      top += stepRows
      if j>0
        draw.push(type: 'line', x1: @LINE_PADDING, y1: y, x2: config.width-@LINE_PADDING, y2: y, color: @LINE_COLOR)
      left = @LINE_PADDING
      captions = config.captions?[j]
      for i in [0...config.cols]
        x = Math.round(left)
        left += stepCols
        if captions?[i]
          draw.push(type: 'text', x: x+@TEXT_PADDING_X, y: Math.round(top-@TEXT_PADDING_Y), color: @TEXT_COLOR, text: captions?[i], size: @TEXT_SIZE)
    return draw

class WeekTemplateConfig extends TemplateConfig
  tmpl: """
  <div class=\"modal hide\">
    <div class=\"modal-header\">
      <h3>Weekly sheet config</h3>
    </div>
    <div class=\"modal-body\">
      <form class=\"form-horizontal\">
        <div class=\"control-group\">
          <label class=\"control-label\">Week starts from:</label>
          <div class=\"controls\">
            <input type=\"date\" class=\"week-dialog-date\" placeholder=\"Date\">
          </div>
        </div>
        <div class=\"control-group\">
          <label class=\"control-label\">Holidays</label>
          <div class=\"controls week-dialog-holidays\">
          </div>
        </div>
      </form>
    </div>
    <div class=\"modal-footer\">
        <a href=\"#\" class=\"btn btn-primary week-dialog-do-save\">Save</a>
        <a href=\"#\" class=\"btn week-dialog-do-close\">Close</a>
    </div>
  </div>
  """
  dayNames: ['日', '月', '火', '水', '木', '金', '土']
  weekStarts: 1
  defaultHolidays: 0: yes, 6: yes

  configure: (tmpl, sheet, controller) ->
    log 'Show template dialog'
    div = $(@tmpl).appendTo(document.body)
    div.modal backdrop: 'static', keyboard: no
    div.find('.week-dialog-do-close').bind 'click', () =>
      div.modal('hide').remove()
    config = sheet.config ? {}
    dateInput = div.find('.week-dialog-date')
    if config.date
      dateInput.val(config.date)
    checkboxes = []
    checkboxesDiv = div.find('.week-dialog-holidays')
    checkboxValues = config.holidays ? @defaultHolidays
    for index in [0...@dayNames.length]
      day = (index+@weekStarts) % @dayNames.length
      label = $(document.createElement('label')).addClass('checkbox inline').appendTo(checkboxesDiv)
      label.text(@dayNames[day])
      checkbox = $(document.createElement('input')).attr(type: 'checkbox', checked: checkboxValues[day] ? no)
      label.prepend checkbox
      checkboxes.push checkbox
    div.find('.week-dialog-do-save').bind 'click', () =>
      date = dateInput.val()
      if not date then return @app.showError 'No date selected'
      log 'Date', date
      config.date = date
      config.holidays = {}
      for index in [0...@dayNames.length]
        day = (index+@weekStarts) % @dayNames.length
        if checkboxes[index].attr 'checked'
          config.holidays[day] = yes
      config.draw = @generate config.date, config.holidays
      sheet.config = config
      @app.manager.save 'sheets', sheet, (err) =>
        if err then return @app.showError err
        controller.reloadSheets()
        div.modal('hide').remove()

  drawStart: 11
  drawTextY: -2
  drawStep: 18
  drawTextX: 85
  drawLineX1: 65
  drawLineX2: 98
  drawLineColor: '#dddddd'
  drawTextColor: '#000000'
  drawTextHolidayColor: '#ff8888'

  generate: (date, holidays) ->
    draw = []
    dt = new Date(date)
    y = @drawStart
    for index in [0...@dayNames.length]
      day = dt.getDay()
      draw.push type: 'line', x1: @drawLineX1, y1: y, x2: @drawLineX2, y2: y, color: @drawLineColor
      textColor = if holidays[day] then @drawTextHolidayColor else @drawTextColor
      dateText = if dt.getDate()<10 then '0'+dt.getDate() else ''+dt.getDate()
      draw.push type: 'text', x: @drawTextX, y: y+@drawTextY, color: textColor, text: (@dayNames[day])+' '+dateText
      dt.setDate(dt.getDate()+1)
      y += @drawStep
    return draw

class DrawTemplate

  constructor: (@app) ->

  render: (tmpl, sheet, canvas, zoom, bounds) ->
    data = sheet?.config?.draw ? (tmpl.config?.draw ? [])
    log 'Draw', data
    @draw data, canvas, zoom, bounds

  draw: (data, canvas, zoom, bounds) ->
    lineParams = (obj, params) ->
      params.lineWidth = obj?.width ? 1
      params.strokeStyle = obj?.color ? '#000000'
    fillParams = (obj, params) ->
      if obj?.fill then params.fillStyle = obj.fill
    textParams = (obj, params) ->
      params.strokeStyle = obj?.color ? '#000000'
      params.fillStyle = obj?.color ? '#000000'
      params.lineWidth = obj?.width ? 1
      fontSize = obj?.size ? 0
      fontPixels = 0
      switch fontSize
        when -4 then fontPixels = zoom*2.0
        when -3 then fontPixels = zoom*2.7
        when -2 then fontPixels = zoom*3.5
        when -1 then fontPixels = zoom*4.5
        when 0 then fontPixels = zoom*5.5
        when 1 then fontPixels = zoom*6.5
        when 2 then fontPixels = zoom*7.5
      params.font = ''+(Math.round(fontPixels*100)/100)+'px Arial'
    processArray = (data, translated) =>
      coord = (value, vert = no) ->
        val = value ? 0
        if val<0 and not translated
          val = (if vert then bounds.height else bounds.width) + val
        return val
      for item in data
        canvas.save()
        params = {}
        switch item.type
          when 'circle'
            lineParams item, params
            canvas.beginPath().arc(coord(item.x)*zoom, coord(item.y, yes)*zoom, item.r*zoom).stroke(params).endPath()
          when 'arc'
            canvas.angleUnit = 'degrees'
            lineParams item, params
            canvas.beginPath().arc(coord(item.x)*zoom, coord(item.y, yes)*zoom, item.r*zoom, item.sa, item.ea).stroke(params).endPath()
          when 'rect'
            lineParams item, params
            fillParams item, params
            x1 = coord(item.x1)*zoom
            y1 = coord(item.y1, yes)*zoom
            x2 = coord(item.x2)*zoom
            y2 = coord(item.y2, yes)*zoom
            canvas.beginPath().rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1-x2), Math.abs(y1-y2)).paint(params).endPath()
          when 'line'
            lineParams item, params
            canvas.beginPath().moveTo(coord(item.x1)*zoom, coord(item.y1, yes)*zoom).lineTo(coord(item.x2)*zoom, coord(item.y2, yes)*zoom).paint(params).endPath()
          when 'text'
            textParams item, params
            canvas.fillText(item.text, coord(item.x)*zoom, coord(item.y, yes)*zoom, params)
          when 'move'
            items = item.items ? []
            canvas.save()
            if item.x and item.y
              canvas.translate(coord(item.x)*zoom, coord(item.y, yes)*zoom)
            if item.a
              canvas.angleUnit = 'degrees'
              canvas.rotate(item.a)
            if item.z
              canvas.scale(item.z/100, item.z/100)
            processArray(items, yes)
            canvas.restore()
          else
            log 'Unknown drawing:', item.type, item
        canvas.restore()
    processArray(data)

class ScoreConfigDialog extends DialogTemplateConfig

  formTmpl: """
  <div style=\"text-align: center;\">
    <button class=\"btn btn-info score-up\">+</button>
    <div style=\"display: inline-block; width: 7em; font-size: 1.2em;\">Score: <span class=\"score-value\" style=\"font-weight: bold;\">+0</span></div>
    <button class=\"btn btn-info score-down\">-</button>
  </div>
  """
  upColor: '#00aa00'
  downColor: '#dd0000'
  zeroColor: '#000000'

  title: 'Enter Score:'
  createForm: (div, tmpl, config) ->
    form = $(@formTmpl).appendTo(div)
    scoreUp = config.scoreUp ? 0
    scoreDown = config.scoreDown ? 0
    score = 0
    updateScore = =>
      if score is 0
        form.find('.score-value').text('0').css(color: @zeroColor)
      else
        if score>0
          form.find('.score-value').text('+'+score).css(color: @upColor)
        else
          form.find('.score-value').text(score).css(color: @downColor)
    form.find('.score-up').bind 'click', =>
      score++
      updateScore()
    form.find('.score-down').bind 'click', =>
      score--
      updateScore()
    updateScore()
    return () =>
      if score>0
        config.scoreUp = scoreUp+score
      else
        config.scoreDown = scoreDown+score
      config.drawscore = @generate(config)

  sizes: [{min: 125, radius: 5}, {min: 25, radius: 4}, {min: 5, radius: 3}, {min: 1, radius: 2}]
  gap: 2
  marginTop: 3
  marginLeft: 4

  generate: (config) ->
    draw = []
    score = config.scoreUp + config.scoreDown
    color = if score>0 then @upColor else @downColor
    score = Math.abs(score)
    index = 0
    x = -@marginLeft
    while score>0
      size = @sizes[index]
      boxes = Math.floor(score/size.min)
      score -= size.min*boxes
      for i in [0...boxes]
        draw.push(type: 'rect', x1: x, y1: -@marginTop, x2: x-size.radius, y2: -@marginTop-size.radius, fill: color, color: color)
        x -= size.radius+@gap
      index++
    return draw

class ScoreSheetPlugin extends SheetPlugin

  constructor: (@app) ->
    @dialog = new ScoreConfigDialog(@app)

  load: (div, sheet, controller) ->
    @addButton div, 'icon-signal', =>
      @dialog.configure(null, sheet, controller)


window.DrawTemplate = DrawTemplate
window.templateConfigs = {
  week: WeekTemplateConfig
  grid: GridTemplateConfig
  chronodex: ChronodexConfig
}

window.sheetPlugins = {
  score: ScoreSheetPlugin
}
