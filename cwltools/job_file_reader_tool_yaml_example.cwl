#!/usr/bin/env cwl-runner
#
# parses a yaml file and returns strongly typed results
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
  - id: foo
    type: string?
  - id: bar
    type: string?
  - id: baz
    type: string?

expression: |
  ${
    // we'd like to import a yaml parser but this doesn't work:
    //import '/node_modules/js-yaml/bin/js-yaml.js';
    var inp = inputs.inputfile.contents;
    var outputs=['foo','bar','baz'];
    var result={};
    for (var i=0; i<outputs.length; i++) {
      var output = outputs[i];
      var patt = new RegExp(output+":[\\s]*(.*)");
      var value = inp.match(patt);
      if (value!=null) result[output]=value[1];
    }
    return result;
  }
