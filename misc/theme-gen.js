(function() {
  var doGen, fs, newValue, pattern, util, _ref, _ref2, _ref3;

  util = require('util');

  fs = require('fs');

  pattern = 'note0';

  newValue = 'note';

  doGen = function(path, start, end) {
    var contents, file, files, i, newContents, newFile, rexp, value, _i, _len, _results;
    rexp = new RegExp(pattern, 'g');
    util.log("doGen: " + path + ", " + start + " - " + end);
    files = fs.readdirSync(path);
    _results = [];
    for (_i = 0, _len = files.length; _i < _len; _i++) {
      file = files[_i];
      if (file.toString().indexOf(pattern) === -1) continue;
      util.log("Process " + file);
      contents = fs.readFileSync(path + '/' + file, 'utf-8');
      _results.push((function() {
        var _results2;
        _results2 = [];
        for (i = start; start <= end ? i <= end : i >= end; start <= end ? i++ : i--) {
          value = newValue + i;
          newFile = path + '/' + file.replace(rexp, value);
          util.log("New file: " + newFile + " " + value);
          newContents = contents.replace(rexp, value);
          _results2.push(fs.writeFileSync(newFile, newContents, 'utf-8'));
        }
        return _results2;
      })());
    }
    return _results;
  };

  util.log('Startup: ' + process.argv);

  doGen((_ref = process.argv[2]) != null ? _ref : '.', parseInt((_ref2 = process.argv[3]) != null ? _ref2 : 1), parseInt((_ref3 = process.argv[4]) != null ? _ref3 : 0));

}).call(this);
