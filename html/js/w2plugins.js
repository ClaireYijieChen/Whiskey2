(function() {
  var ChronodexConfig, DialogTemplateConfig, DrawTemplate, GridTemplateConfig, ScoreConfigDialog, ScoreSheetPlugin, WeekTemplateConfig,
    __hasProp = Object.prototype.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor; child.__super__ = parent.prototype; return child; };

  DialogTemplateConfig = (function(_super) {

    __extends(DialogTemplateConfig, _super);

    function DialogTemplateConfig() {
      DialogTemplateConfig.__super__.constructor.apply(this, arguments);
    }

    DialogTemplateConfig.prototype.title = 'Sheet config';

    DialogTemplateConfig.prototype.tmpl = "<div class=\"modal hide\">\n  <div class=\"modal-header\">\n    <h3>Grid sheet config</h3>\n  </div>\n  <div class=\"modal-body\">\n  </div>\n  <div class=\"modal-footer\">\n      <a href=\"#\" class=\"btn btn-primary dialog-do-save\">Save</a>\n      <a href=\"#\" class=\"btn dialog-do-close\">Close</a>\n  </div>\n</div>";

    DialogTemplateConfig.prototype.sheetSizeTmpl = "<form class=\"form-horizontal\">\n  <div class=\"control-group\">\n    <label class=\"control-label\" for=\"main-login\">Page size:</label>\n    <div class=\"controls\">\n      <select class=\"size-select\">\n      </select>\n    </div>\n  </div>\n  <div class=\"control-group\">\n    <label class=\"control-label\" for=\"main-password\">Orientation:</label>\n    <div class=\"controls\">\n      <select class=\"orientation-select\">\n        <option value=\"0\">Portrait</option>\n        <option value=\"1\">Landscape</option>\n      </select>\n    </div>\n  </div>\n</form>";

    DialogTemplateConfig.prototype.sizes = [
      {
        caption: 'A6',
        width: 102,
        height: 144
      }, {
        caption: 'A5',
        width: 144,
        height: 204
      }, {
        caption: 'A4',
        width: 204,
        height: 288
      }, {
        caption: 'A3',
        width: 288,
        height: 408
      }
    ];

    DialogTemplateConfig.prototype.createForm = function(div, tmpl, config) {
      return function() {};
    };

    DialogTemplateConfig.prototype.createSheetSizeForm = function(div, tmpl, config) {
      var form, i, option, orientationSelect, size, sizeSelect, _ref, _ref2, _ref3,
        _this = this;
      form = $(this.sheetSizeTmpl).appendTo(div);
      config.size = (_ref = config.size) != null ? _ref : 0;
      config.orientation = (_ref2 = config.orientation) != null ? _ref2 : 0;
      sizeSelect = form.find('.size-select');
      for (i = 0, _ref3 = this.sizes.length; 0 <= _ref3 ? i < _ref3 : i > _ref3; 0 <= _ref3 ? i++ : i--) {
        size = this.sizes[i];
        option = $(document.createElement('option')).appendTo(sizeSelect);
        option.attr({
          value: i
        });
        option.text(size.caption);
      }
      sizeSelect.val(config.size);
      orientationSelect = form.find('.orientation-select');
      orientationSelect.val(config.orientation);
      return function() {
        var _ref4;
        config.size = parseInt(sizeSelect.val());
        config.orientation = parseInt(orientationSelect.val());
        size = _this.sizes[config.size];
        if (size) {
          config.width = size.width;
          config.height = size.height;
          if (config.orientation === 1) {
            return _ref4 = [config.height, config.width], config.width = _ref4[0], config.height = _ref4[1], _ref4;
          }
        }
      };
    };

    DialogTemplateConfig.prototype.configure = function(tmpl, sheet, controller) {
      var config, div, fn, _ref,
        _this = this;
      div = $(this.tmpl).appendTo(document.body);
      div.find('.modal-header h3').text(this.title);
      div.modal({
        backdrop: 'static',
        keyboard: false
      });
      div.find('.dialog-do-close').bind('click', function() {
        return div.modal('hide').remove();
      });
      config = (_ref = sheet.config) != null ? _ref : {};
      fn = this.createForm(div.find('.modal-body'), tmpl, config);
      return div.find('.dialog-do-save').bind('click', function() {
        fn();
        sheet.config = config;
        return _this.app.manager.save('sheets', sheet, function(err) {
          if (err) return _this.app.showError(err);
          controller.reloadSheets();
          return div.modal('hide').remove();
        });
      });
    };

    return DialogTemplateConfig;

  })(TemplateConfig);

  ChronodexConfig = (function(_super) {

    __extends(ChronodexConfig, _super);

    function ChronodexConfig() {
      ChronodexConfig.__super__.constructor.apply(this, arguments);
    }

    ChronodexConfig.prototype.title = 'Chronodex config';

    ChronodexConfig.prototype.formTmpl = "<form class=\"form-horizontal\">\n  <div class=\"control-group\">\n    <label class=\"control-label\">Position:</label>\n    <div class=\"controls\">\n      <select class=\"align-select\">\n        <option value=\"l\">Left</option>\n        <option value=\"c\">Center</option>\n        <option value=\"r\">Right</option>\n      </select>\n      <select class=\"valign-select\">\n        <option value=\"t\">Top</option>\n        <option value=\"m\">Middle</option>\n        <option value=\"b\">Bottom</option>\n      </select>\n    </div>\n  </div>\n  <div class=\"control-group\">\n    <label class=\"control-label\">Zoom (%):</label>\n    <div class=\"controls\">\n      <input type=\"text\" class=\"zoom-text\"/>\n    </div>\n  </div>\n</form>";

    ChronodexConfig.prototype.createForm = function(div, tmpl, config) {
      var alignSelect, form, sizeFn, valignSelect, zoomText, _ref, _ref2, _ref3,
        _this = this;
      form = $(this.formTmpl).appendTo(div);
      config.align = (_ref = config.align) != null ? _ref : 'c';
      config.valign = (_ref2 = config.valign) != null ? _ref2 : 'm';
      config.zoom = (_ref3 = config.zoom) != null ? _ref3 : 100;
      alignSelect = form.find('.align-select').val(config.align);
      valignSelect = form.find('.valign-select').val(config.valign);
      zoomText = form.find('.zoom-text').val(config.zoom);
      sizeFn = this.createSheetSizeForm(div, tmpl, config);
      return function() {
        var _ref4;
        config.align = alignSelect.val();
        config.valign = valignSelect.val();
        config.zoom = (_ref4 = parseInt(zoomText.val())) != null ? _ref4 : 100;
        sizeFn();
        return config.draw = _this.generate(config);
      };
    };

    ChronodexConfig.prototype.generate = function(config) {
      var bigRadius, centerx, centery, circleRadius, deg, degStep, draw, height, heights, i, itm, lineColor, lineGrayColor, littleRadius, middleRadius, pi180, smallCRadius, smallRadius, spotHeights, time, times, x, xyWithAngle, y, zoom, _ref, _ref2, _ref3, _ref4,
        _this = this;
      draw = [];
      centerx = config.width / 2;
      centery = config.height / 2;
      circleRadius = 10;
      smallCRadius = 2;
      smallRadius = 15;
      middleRadius = 20;
      bigRadius = 25;
      degStep = 30;
      heights = [smallRadius, middleRadius, bigRadius];
      spotHeights = [bigRadius, middleRadius, bigRadius];
      lineColor = '#000000';
      lineGrayColor = '#aaaaaa';
      littleRadius = 6;
      config.spots = [];
      times = [
        {
          x: 0,
          y: littleRadius - 1,
          t: '6am'
        }, {
          x: -3,
          y: littleRadius - 4,
          t: '7am'
        }, {
          x: -littleRadius + 1,
          y: -1,
          t: '8am'
        }, {
          x: -smallRadius - 6,
          y: -1,
          t: '9am'
        }, {
          x: -middleRadius - 6,
          y: -smallRadius + 4,
          t: '10am'
        }, {
          x: -middleRadius,
          y: -bigRadius + 2,
          t: '11am'
        }, {
          x: 0,
          y: -bigRadius - 3,
          t: '12am'
        }, {
          x: smallRadius - 3,
          y: -middleRadius + 2,
          t: '1pm'
        }, {
          x: middleRadius + 2,
          y: -smallRadius,
          t: '2pm'
        }, {
          x: bigRadius + 3,
          y: 0,
          t: '3pm'
        }, {
          x: middleRadius - 1,
          y: smallRadius - 2,
          t: '4pm'
        }, {
          x: smallRadius - 2,
          y: bigRadius,
          t: '5pm'
        }, {
          x: 0,
          y: bigRadius + 5,
          t: '6pm'
        }, {
          x: -smallRadius,
          y: middleRadius,
          t: '7pm'
        }, {
          x: -bigRadius,
          y: smallRadius,
          t: '8pm'
        }, {
          x: -bigRadius - 8,
          y: 0,
          t: '9pm'
        }
      ];
      draw.push({
        type: 'circle',
        color: lineColor,
        x: bigRadius,
        y: 0,
        r: smallCRadius
      });
      draw.push({
        type: 'circle',
        color: lineColor,
        x: 0,
        y: bigRadius,
        r: smallCRadius
      });
      draw.push({
        type: 'circle',
        color: lineColor,
        x: -bigRadius,
        y: 0,
        r: smallCRadius
      });
      draw.push({
        type: 'circle',
        color: lineColor,
        x: 0,
        y: -bigRadius,
        r: smallCRadius
      });
      draw.push({
        type: 'circle',
        color: lineColor,
        x: 0,
        y: 0,
        r: circleRadius
      });
      draw.push({
        type: 'arc',
        color: lineGrayColor,
        x: 0,
        y: 0,
        r: littleRadius,
        sa: 90,
        ea: 180
      });
      pi180 = Math.PI / 180;
      zoom = config.zoom / 100;
      xyWithAngle = function(deg, radius) {
        return [_this.preciseCoord(centerx + Math.round(Math.cos(deg * pi180) * radius * zoom)), _this.preciseCoord(centery + Math.round(Math.sin(deg * pi180) * radius * zoom))];
      };
      deg = 90;
      for (i = 0; i < 4; i++) {
        itm = {
          type: 'move',
          a: deg,
          items: [
            {
              type: 'line',
              x1: littleRadius,
              y1: 0,
              x2: circleRadius,
              y2: 0,
              color: lineGrayColor
            }
          ]
        };
        draw.push(itm);
        _ref = xyWithAngle(deg, circleRadius), x = _ref[0], y = _ref[1];
        config.spots.push({
          id: 'h' + (6 + i),
          x: x,
          y: y
        });
        deg += degStep;
      }
      deg = 0;
      for (i = 0; i < 12; i++) {
        height = heights[i % heights.length];
        _ref2 = xyWithAngle(deg, spotHeights[i % spotHeights.length]), x = _ref2[0], y = _ref2[1];
        config.spots.push({
          id: 'h' + (i > 7 ? i + 2 : i + 15),
          x: x,
          y: y
        });
        draw.push({
          type: 'arc',
          color: lineColor,
          x: 0,
          y: 0,
          r: height,
          sa: deg,
          ea: deg + degStep
        });
        draw.push({
          type: 'move',
          a: deg,
          items: [
            {
              type: 'line',
              x1: circleRadius,
              y1: 0,
              x2: height,
              y2: 0,
              color: lineColor
            }
          ]
        });
        draw.push(itm);
        deg += degStep;
        draw.push({
          type: 'move',
          a: deg,
          items: [
            {
              type: 'line',
              x1: circleRadius,
              y1: 0,
              x2: height,
              y2: 0,
              color: lineColor
            }
          ]
        });
      }
      for (i in times) {
        time = times[i];
        draw.push({
          type: 'text',
          text: time.t,
          x: (_ref3 = time.x) != null ? _ref3 : 0,
          y: (_ref4 = time.y) != null ? _ref4 : 0,
          size: -3
        });
      }
      return [
        {
          type: 'move',
          x: centerx,
          y: centery,
          items: draw,
          z: config.zoom
        }
      ];
    };

    return ChronodexConfig;

  })(DialogTemplateConfig);

  GridTemplateConfig = (function(_super) {

    __extends(GridTemplateConfig, _super);

    function GridTemplateConfig() {
      GridTemplateConfig.__super__.constructor.apply(this, arguments);
    }

    GridTemplateConfig.prototype.tmpl = "<div class=\"modal hide\">\n  <div class=\"modal-header\">\n    <h3>Grid sheet config</h3>\n  </div>\n  <div class=\"modal-body\">\n    <table class=\"grid-table\" style=\"width: 100%;\">\n      <tbody></tbody>\n    </table>\n    <div>\n      <a href=\"#\" class=\"btn btn-success dialog-do-add-row\">+ Row</a>\n      <a href=\"#\" class=\"btn btn-success dialog-do-add-col\">+ Column</a>\n      <a href=\"#\" class=\"btn btn-danger dialog-do-remove-row\">- Row</a>\n      <a href=\"#\" class=\"btn btn-danger dialog-do-remove-col\">- Column</a>\n    </div>\n    <div style=\"margin-top: 0.5em;\">\n      <select class=\"size-select\">\n      </select>\n      <select class=\"orientation-select\">\n        <option value=\"0\">Portrait</option>\n        <option value=\"1\">Landscape</option>\n      </select>\n    </div>\n  </div>\n  <div class=\"modal-footer\">\n      <a href=\"#\" class=\"btn btn-primary dialog-do-save\">Save</a>\n      <a href=\"#\" class=\"btn dialog-do-close\">Close</a>\n  </div>\n</div>";

    GridTemplateConfig.prototype.sizes = [
      {
        caption: 'A6',
        width: 102,
        height: 144
      }, {
        caption: 'A5',
        width: 144,
        height: 204
      }, {
        caption: 'A4',
        width: 204,
        height: 288
      }, {
        caption: 'A3',
        width: 288,
        height: 408
      }
    ];

    GridTemplateConfig.prototype.configure = function(tmpl, sheet, controller) {
      var config, div, i, inputs, option, orientationSelect, refreshTable, saveTable, size, sizeSelect, _ref, _ref2, _ref3, _ref4, _ref5, _ref6,
        _this = this;
      div = $(this.tmpl).appendTo(document.body);
      div.modal({
        backdrop: 'static',
        keyboard: false
      });
      div.find('.dialog-do-close').bind('click', function() {
        return div.modal('hide').remove();
      });
      config = (_ref = sheet.config) != null ? _ref : {};
      config.cols = (_ref2 = config.cols) != null ? _ref2 : 2;
      config.rows = (_ref3 = config.rows) != null ? _ref3 : 2;
      config.size = (_ref4 = config.size) != null ? _ref4 : 0;
      config.orientation = (_ref5 = config.orientation) != null ? _ref5 : 0;
      inputs = [];
      saveTable = function() {
        var captions, cols, i, j, row, rows, _results;
        cols = config.cols;
        rows = config.rows;
        config.captions = [];
        _results = [];
        for (j = 0; 0 <= rows ? j < rows : j > rows; 0 <= rows ? j++ : j--) {
          row = inputs[j];
          captions = [];
          config.captions.push(captions);
          _results.push((function() {
            var _results2;
            _results2 = [];
            for (i = 0; 0 <= cols ? i < cols : i > cols; 0 <= cols ? i++ : i--) {
              captions[i] = '';
              if (row && row[i]) {
                _results2.push(captions[i] = row[i].val().trim());
              } else {
                _results2.push(void 0);
              }
            }
            return _results2;
          })());
        }
        return _results;
      };
      refreshTable = function() {
        var captions, cols, i, input, inputsRow, j, rows, tbody, td, tr, width, _ref6, _results;
        cols = config.cols;
        rows = config.rows;
        tbody = div.find('.grid-table tbody').empty();
        inputs = [];
        _results = [];
        for (j = 0; 0 <= rows ? j < rows : j > rows; 0 <= rows ? j++ : j--) {
          tr = $(document.createElement('tr')).appendTo(tbody);
          inputsRow = [];
          inputs.push(inputsRow);
          captions = (_ref6 = config.captions) != null ? _ref6[j] : void 0;
          _results.push((function() {
            var _results2;
            _results2 = [];
            for (i = 0; 0 <= cols ? i < cols : i > cols; 0 <= cols ? i++ : i--) {
              width = Math.floor(100 / cols);
              td = $(document.createElement('td')).appendTo(tr);
              td.width("" + width + "%");
              input = $(document.createElement('input')).attr({
                type: 'text'
              }).appendTo(td);
              input.css({
                width: '100%',
                'margin-right': '4px',
                'padding': '4px 0px',
                'margin-top': '9px'
              });
              if (captions != null ? captions[i] : void 0) {
                input.val(captions != null ? captions[i] : void 0);
              }
              _results2.push(inputsRow.push(input));
            }
            return _results2;
          })());
        }
        return _results;
      };
      sizeSelect = div.find('.size-select');
      for (i = 0, _ref6 = this.sizes.length; 0 <= _ref6 ? i < _ref6 : i > _ref6; 0 <= _ref6 ? i++ : i--) {
        size = this.sizes[i];
        option = $(document.createElement('option')).appendTo(sizeSelect);
        option.attr({
          value: i
        });
        option.text(size.caption);
      }
      sizeSelect.val(config.size);
      orientationSelect = div.find('.orientation-select');
      orientationSelect.val(config.orientation);
      refreshTable();
      div.find('.dialog-do-add-row').bind('click', function() {
        config.rows++;
        saveTable();
        return refreshTable();
      });
      div.find('.dialog-do-add-col').bind('click', function() {
        config.cols++;
        saveTable();
        return refreshTable();
      });
      div.find('.dialog-do-remove-row').bind('click', function() {
        if (config.rows > 1) {
          config.rows--;
          saveTable();
          return refreshTable();
        }
      });
      div.find('.dialog-do-remove-col').bind('click', function() {
        if (config.cols > 1) {
          config.cols--;
          saveTable();
          return refreshTable();
        }
      });
      return div.find('.dialog-do-save').bind('click', function() {
        var _ref7;
        saveTable();
        config.size = parseInt(sizeSelect.val());
        config.orientation = parseInt(orientationSelect.val());
        size = _this.sizes[config.size];
        if (size) {
          config.width = size.width;
          config.height = size.height;
          if (config.orientation === 1) {
            _ref7 = [config.height, config.width], config.width = _ref7[0], config.height = _ref7[1];
          }
        }
        config.draw = _this.generate(config);
        sheet.config = config;
        return _this.app.manager.save('sheets', sheet, function(err) {
          if (err) return _this.app.showError(err);
          controller.reloadSheets();
          return div.modal('hide').remove();
        });
      });
    };

    GridTemplateConfig.prototype.LINE_PADDING = 3;

    GridTemplateConfig.prototype.TEXT_PADDING_X = 3;

    GridTemplateConfig.prototype.TEXT_PADDING_Y = 3;

    GridTemplateConfig.prototype.LINE_COLOR = '#dddddd';

    GridTemplateConfig.prototype.TEXT_COLOR = '#aaaaaa';

    GridTemplateConfig.prototype.TEXT_SIZE = -1;

    GridTemplateConfig.prototype.generate = function(config) {
      var captions, draw, i, j, left, stepCols, stepRows, top, x, y, _ref, _ref2, _ref3, _ref4;
      draw = [];
      stepRows = (config.height - 2 * this.LINE_PADDING) / config.rows;
      stepCols = (config.width - 2 * this.LINE_PADDING) / config.cols;
      left = this.LINE_PADDING;
      for (i = 0, _ref = config.cols; 0 <= _ref ? i < _ref : i > _ref; 0 <= _ref ? i++ : i--) {
        x = Math.round(left);
        left += stepCols;
        if (i > 0) {
          draw.push({
            type: 'line',
            x1: x,
            y1: this.LINE_PADDING,
            x2: x,
            y2: config.height - this.LINE_PADDING,
            color: this.LINE_COLOR
          });
        }
      }
      top = this.LINE_PADDING;
      for (j = 0, _ref2 = config.rows; 0 <= _ref2 ? j < _ref2 : j > _ref2; 0 <= _ref2 ? j++ : j--) {
        y = Math.round(top);
        top += stepRows;
        if (j > 0) {
          draw.push({
            type: 'line',
            x1: this.LINE_PADDING,
            y1: y,
            x2: config.width - this.LINE_PADDING,
            y2: y,
            color: this.LINE_COLOR
          });
        }
        left = this.LINE_PADDING;
        captions = (_ref3 = config.captions) != null ? _ref3[j] : void 0;
        for (i = 0, _ref4 = config.cols; 0 <= _ref4 ? i < _ref4 : i > _ref4; 0 <= _ref4 ? i++ : i--) {
          x = Math.round(left);
          left += stepCols;
          if (captions != null ? captions[i] : void 0) {
            draw.push({
              type: 'text',
              x: x + this.TEXT_PADDING_X,
              y: Math.round(top - this.TEXT_PADDING_Y),
              color: this.TEXT_COLOR,
              text: captions != null ? captions[i] : void 0,
              size: this.TEXT_SIZE
            });
          }
        }
      }
      return draw;
    };

    return GridTemplateConfig;

  })(TemplateConfig);

  WeekTemplateConfig = (function(_super) {

    __extends(WeekTemplateConfig, _super);

    function WeekTemplateConfig() {
      WeekTemplateConfig.__super__.constructor.apply(this, arguments);
    }

    WeekTemplateConfig.prototype.tmpl = "<div class=\"modal hide\">\n  <div class=\"modal-header\">\n    <h3>Weekly sheet config</h3>\n  </div>\n  <div class=\"modal-body\">\n    <form class=\"form-horizontal\">\n      <div class=\"control-group\">\n        <label class=\"control-label\">Week starts from:</label>\n        <div class=\"controls\">\n          <input type=\"date\" class=\"week-dialog-date\" placeholder=\"Date\">\n        </div>\n      </div>\n      <div class=\"control-group\">\n        <label class=\"control-label\">Holidays</label>\n        <div class=\"controls week-dialog-holidays\">\n        </div>\n      </div>\n    </form>\n  </div>\n  <div class=\"modal-footer\">\n      <a href=\"#\" class=\"btn btn-primary week-dialog-do-save\">Save</a>\n      <a href=\"#\" class=\"btn week-dialog-do-close\">Close</a>\n  </div>\n</div>";

    WeekTemplateConfig.prototype.dayNames = ['日', '月', '火', '水', '木', '金', '土'];

    WeekTemplateConfig.prototype.weekStarts = 1;

    WeekTemplateConfig.prototype.defaultHolidays = {
      0: true,
      6: true
    };

    WeekTemplateConfig.prototype.configure = function(tmpl, sheet, controller) {
      var checkbox, checkboxValues, checkboxes, checkboxesDiv, config, dateInput, day, div, index, label, _ref, _ref2, _ref3, _ref4,
        _this = this;
      log('Show template dialog');
      div = $(this.tmpl).appendTo(document.body);
      div.modal({
        backdrop: 'static',
        keyboard: false
      });
      div.find('.week-dialog-do-close').bind('click', function() {
        return div.modal('hide').remove();
      });
      config = (_ref = sheet.config) != null ? _ref : {};
      dateInput = div.find('.week-dialog-date');
      if (config.date) dateInput.val(config.date);
      checkboxes = [];
      checkboxesDiv = div.find('.week-dialog-holidays');
      checkboxValues = (_ref2 = config.holidays) != null ? _ref2 : this.defaultHolidays;
      for (index = 0, _ref3 = this.dayNames.length; 0 <= _ref3 ? index < _ref3 : index > _ref3; 0 <= _ref3 ? index++ : index--) {
        day = (index + this.weekStarts) % this.dayNames.length;
        label = $(document.createElement('label')).addClass('checkbox inline').appendTo(checkboxesDiv);
        label.text(this.dayNames[day]);
        checkbox = $(document.createElement('input')).attr({
          type: 'checkbox',
          checked: (_ref4 = checkboxValues[day]) != null ? _ref4 : false
        });
        label.prepend(checkbox);
        checkboxes.push(checkbox);
      }
      return div.find('.week-dialog-do-save').bind('click', function() {
        var date, index, _ref5;
        date = dateInput.val();
        if (!date) return _this.app.showError('No date selected');
        log('Date', date);
        config.date = date;
        config.holidays = {};
        for (index = 0, _ref5 = _this.dayNames.length; 0 <= _ref5 ? index < _ref5 : index > _ref5; 0 <= _ref5 ? index++ : index--) {
          day = (index + _this.weekStarts) % _this.dayNames.length;
          if (checkboxes[index].attr('checked')) config.holidays[day] = true;
        }
        config.draw = _this.generate(config.date, config.holidays);
        sheet.config = config;
        return _this.app.manager.save('sheets', sheet, function(err) {
          if (err) return _this.app.showError(err);
          controller.reloadSheets();
          return div.modal('hide').remove();
        });
      });
    };

    WeekTemplateConfig.prototype.drawStart = 11;

    WeekTemplateConfig.prototype.drawTextY = -2;

    WeekTemplateConfig.prototype.drawStep = 18;

    WeekTemplateConfig.prototype.drawTextX = 85;

    WeekTemplateConfig.prototype.drawLineX1 = 65;

    WeekTemplateConfig.prototype.drawLineX2 = 98;

    WeekTemplateConfig.prototype.drawLineColor = '#dddddd';

    WeekTemplateConfig.prototype.drawTextColor = '#000000';

    WeekTemplateConfig.prototype.drawTextHolidayColor = '#ff8888';

    WeekTemplateConfig.prototype.generate = function(date, holidays) {
      var dateText, day, draw, dt, index, textColor, y, _ref;
      draw = [];
      dt = new Date(date);
      y = this.drawStart;
      for (index = 0, _ref = this.dayNames.length; 0 <= _ref ? index < _ref : index > _ref; 0 <= _ref ? index++ : index--) {
        day = dt.getDay();
        draw.push({
          type: 'line',
          x1: this.drawLineX1,
          y1: y,
          x2: this.drawLineX2,
          y2: y,
          color: this.drawLineColor
        });
        textColor = holidays[day] ? this.drawTextHolidayColor : this.drawTextColor;
        dateText = dt.getDate() < 10 ? '0' + dt.getDate() : '' + dt.getDate();
        draw.push({
          type: 'text',
          x: this.drawTextX,
          y: y + this.drawTextY,
          color: textColor,
          text: this.dayNames[day] + ' ' + dateText
        });
        dt.setDate(dt.getDate() + 1);
        y += this.drawStep;
      }
      return draw;
    };

    return WeekTemplateConfig;

  })(TemplateConfig);

  DrawTemplate = (function() {

    function DrawTemplate(app) {
      this.app = app;
    }

    DrawTemplate.prototype.render = function(tmpl, sheet, canvas, zoom, bounds) {
      var data, _ref, _ref2, _ref3, _ref4;
      data = (_ref = sheet != null ? (_ref2 = sheet.config) != null ? _ref2.draw : void 0 : void 0) != null ? _ref : (_ref3 = (_ref4 = tmpl.config) != null ? _ref4.draw : void 0) != null ? _ref3 : [];
      log('Draw', data);
      return this.draw(data, canvas, zoom, bounds);
    };

    DrawTemplate.prototype.draw = function(data, canvas, zoom, bounds) {
      var fillParams, lineParams, processArray, textParams,
        _this = this;
      lineParams = function(obj, params) {
        var _ref, _ref2;
        params.lineWidth = (_ref = obj != null ? obj.width : void 0) != null ? _ref : 1;
        return params.strokeStyle = (_ref2 = obj != null ? obj.color : void 0) != null ? _ref2 : '#000000';
      };
      fillParams = function(obj, params) {
        if (obj != null ? obj.fill : void 0) return params.fillStyle = obj.fill;
      };
      textParams = function(obj, params) {
        var fontPixels, fontSize, _ref, _ref2, _ref3, _ref4;
        params.strokeStyle = (_ref = obj != null ? obj.color : void 0) != null ? _ref : '#000000';
        params.fillStyle = (_ref2 = obj != null ? obj.color : void 0) != null ? _ref2 : '#000000';
        params.lineWidth = (_ref3 = obj != null ? obj.width : void 0) != null ? _ref3 : 1;
        fontSize = (_ref4 = obj != null ? obj.size : void 0) != null ? _ref4 : 0;
        fontPixels = 0;
        switch (fontSize) {
          case -4:
            fontPixels = zoom * 2.0;
            break;
          case -3:
            fontPixels = zoom * 2.7;
            break;
          case -2:
            fontPixels = zoom * 3.5;
            break;
          case -1:
            fontPixels = zoom * 4.5;
            break;
          case 0:
            fontPixels = zoom * 5.5;
            break;
          case 1:
            fontPixels = zoom * 6.5;
            break;
          case 2:
            fontPixels = zoom * 7.5;
        }
        return params.font = '' + (Math.round(fontPixels * 100) / 100) + 'px Arial';
      };
      processArray = function(data, translated) {
        var coord, item, items, params, x1, x2, y1, y2, _i, _len, _ref, _results;
        coord = function(value, vert) {
          var val;
          if (vert == null) vert = false;
          val = value != null ? value : 0;
          if (val < 0 && !translated) {
            val = (vert ? bounds.height : bounds.width) + val;
          }
          return val;
        };
        _results = [];
        for (_i = 0, _len = data.length; _i < _len; _i++) {
          item = data[_i];
          canvas.save();
          params = {};
          switch (item.type) {
            case 'circle':
              lineParams(item, params);
              canvas.beginPath().arc(coord(item.x) * zoom, coord(item.y, true) * zoom, item.r * zoom).stroke(params).endPath();
              break;
            case 'arc':
              canvas.angleUnit = 'degrees';
              lineParams(item, params);
              canvas.beginPath().arc(coord(item.x) * zoom, coord(item.y, true) * zoom, item.r * zoom, item.sa, item.ea).stroke(params).endPath();
              break;
            case 'rect':
              lineParams(item, params);
              fillParams(item, params);
              x1 = coord(item.x1) * zoom;
              y1 = coord(item.y1, true) * zoom;
              x2 = coord(item.x2) * zoom;
              y2 = coord(item.y2, true) * zoom;
              canvas.beginPath().rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2)).paint(params).endPath();
              break;
            case 'line':
              lineParams(item, params);
              canvas.beginPath().moveTo(coord(item.x1) * zoom, coord(item.y1, true) * zoom).lineTo(coord(item.x2) * zoom, coord(item.y2, true) * zoom).paint(params).endPath();
              break;
            case 'text':
              textParams(item, params);
              canvas.fillText(item.text, coord(item.x) * zoom, coord(item.y, true) * zoom, params);
              break;
            case 'move':
              items = (_ref = item.items) != null ? _ref : [];
              canvas.save();
              if (item.x && item.y) {
                canvas.translate(coord(item.x) * zoom, coord(item.y, true) * zoom);
              }
              if (item.a) {
                canvas.angleUnit = 'degrees';
                canvas.rotate(item.a);
              }
              if (item.z) canvas.scale(item.z / 100, item.z / 100);
              processArray(items, true);
              canvas.restore();
              break;
            default:
              log('Unknown drawing:', item.type, item);
          }
          _results.push(canvas.restore());
        }
        return _results;
      };
      return processArray(data);
    };

    return DrawTemplate;

  })();

  ScoreConfigDialog = (function(_super) {

    __extends(ScoreConfigDialog, _super);

    function ScoreConfigDialog() {
      ScoreConfigDialog.__super__.constructor.apply(this, arguments);
    }

    ScoreConfigDialog.prototype.formTmpl = "<div style=\"text-align: center;\">\n  <button class=\"btn btn-info score-up\">+</button>\n  <div style=\"display: inline-block; width: 7em; font-size: 1.2em;\">Score: <span class=\"score-value\" style=\"font-weight: bold;\">+0</span></div>\n  <button class=\"btn btn-info score-down\">-</button>\n</div>";

    ScoreConfigDialog.prototype.upColor = '#00aa00';

    ScoreConfigDialog.prototype.downColor = '#dd0000';

    ScoreConfigDialog.prototype.zeroColor = '#000000';

    ScoreConfigDialog.prototype.title = 'Enter Score:';

    ScoreConfigDialog.prototype.createForm = function(div, tmpl, config) {
      var form, score, scoreDown, scoreUp, updateScore, _ref, _ref2,
        _this = this;
      form = $(this.formTmpl).appendTo(div);
      scoreUp = (_ref = config.scoreUp) != null ? _ref : 0;
      scoreDown = (_ref2 = config.scoreDown) != null ? _ref2 : 0;
      score = 0;
      updateScore = function() {
        if (score === 0) {
          return form.find('.score-value').text('0').css({
            color: _this.zeroColor
          });
        } else {
          if (score > 0) {
            return form.find('.score-value').text('+' + score).css({
              color: _this.upColor
            });
          } else {
            return form.find('.score-value').text(score).css({
              color: _this.downColor
            });
          }
        }
      };
      form.find('.score-up').bind('click', function() {
        score++;
        return updateScore();
      });
      form.find('.score-down').bind('click', function() {
        score--;
        return updateScore();
      });
      updateScore();
      return function() {
        if (score > 0) {
          config.scoreUp = scoreUp + score;
        } else {
          config.scoreDown = scoreDown + score;
        }
        return config.drawscore = _this.generate(config);
      };
    };

    ScoreConfigDialog.prototype.sizes = [
      {
        min: 125,
        radius: 5
      }, {
        min: 25,
        radius: 4
      }, {
        min: 5,
        radius: 3
      }, {
        min: 1,
        radius: 2
      }
    ];

    ScoreConfigDialog.prototype.gap = 2;

    ScoreConfigDialog.prototype.marginTop = 3;

    ScoreConfigDialog.prototype.marginLeft = 4;

    ScoreConfigDialog.prototype.generate = function(config) {
      var boxes, color, draw, i, index, score, size, x;
      draw = [];
      score = config.scoreUp + config.scoreDown;
      color = score > 0 ? this.upColor : this.downColor;
      score = Math.abs(score);
      index = 0;
      x = -this.marginLeft;
      while (score > 0) {
        size = this.sizes[index];
        boxes = Math.floor(score / size.min);
        score -= size.min * boxes;
        for (i = 0; 0 <= boxes ? i < boxes : i > boxes; 0 <= boxes ? i++ : i--) {
          draw.push({
            type: 'rect',
            x1: x,
            y1: -this.marginTop,
            x2: x - size.radius,
            y2: -this.marginTop - size.radius,
            fill: color,
            color: color
          });
          x -= size.radius + this.gap;
        }
        index++;
      }
      return draw;
    };

    return ScoreConfigDialog;

  })(DialogTemplateConfig);

  ScoreSheetPlugin = (function(_super) {

    __extends(ScoreSheetPlugin, _super);

    function ScoreSheetPlugin(app) {
      this.app = app;
      this.dialog = new ScoreConfigDialog(this.app);
    }

    ScoreSheetPlugin.prototype.load = function(div, sheet, controller) {
      var _this = this;
      return this.addButton(div, 'icon-signal', function() {
        return _this.dialog.configure(null, sheet, controller);
      });
    };

    return ScoreSheetPlugin;

  })(SheetPlugin);

  window.DrawTemplate = DrawTemplate;

  window.templateConfigs = {
    week: WeekTemplateConfig,
    grid: GridTemplateConfig,
    chronodex: ChronodexConfig
  };

  window.sheetPlugins = {
    score: ScoreSheetPlugin
  };

}).call(this);
