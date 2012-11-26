util = require 'util'
fs = require 'fs'

pattern = 'note0'
newValue = 'note'
doGen = (path, start, end) ->
  rexp = new RegExp pattern, 'g'
  util.log "doGen: #{path}, #{start} - #{end}"
  files = fs.readdirSync path
  for file in files
    if file.toString().indexOf(pattern) is -1 then continue
    util.log "Process #{file}"
    contents = fs.readFileSync path+'/'+file, 'utf-8'
    for i in [start..end]
      value = newValue+i
      newFile = path+'/'+file.replace(rexp, value)
      util.log "New file: #{newFile} #{value}"
      newContents = contents.replace rexp, value
      fs.writeFileSync newFile, newContents, 'utf-8'

util.log 'Startup: '+process.argv
doGen (process.argv[2] ? '.'), parseInt(process.argv[3] ? 1), parseInt(process.argv[4] ? 0)