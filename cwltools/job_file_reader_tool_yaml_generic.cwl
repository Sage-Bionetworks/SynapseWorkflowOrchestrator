#!/usr/bin/env cwl-runner
#
# parses a yaml file and returns a map of keys to values
# note: only handles primitive values, not arrays or records
#
cwlVersion: v1.0
class: ExpressionTool

requirements:
  - class: InlineJavascriptRequirement

inputs:
  - id: inputfile
    type: File
    inputBinding:
      loadContents: true

outputs:
  - id: content
    type: string?

expression: |
  ${
    // we'd like to import a yaml parser but this doesn't work:
    //import '/node_modules/js-yaml/bin/js-yaml.js';
    var inp = inputs.inputfile.contents;
    var nameMatchingPattern = new RegExp("(.*):");
    var valueMatchingPattern = new RegExp(":[\\s]*(.*)");
    var lines = inp.match(/.+/g)||[]; // TODO loses the last line if no final end line
    var result={};
    for (var i=0; i<lines.length; i++) {
      var name = lines[i].match(nameMatchingPattern);
      var value = lines[i].match(valueMatchingPattern);
      if (name!=null && value!=null) result[name[1]]=value[1];
    }
    return {"content":result};
  }
