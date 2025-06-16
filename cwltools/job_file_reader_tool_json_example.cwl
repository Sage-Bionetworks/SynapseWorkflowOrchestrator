#!/usr/bin/env cwl-runner
#
# parses a json file and returns strongly typed results
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
    type:
      type: record
      fields:
        - name: a
          type: string
        - name: b
          type: int
  - id: baz
    type: string?

expression: |
  ${
    var j = JSON.parse(inputs.inputfile.contents);
    var outputs=['foo','bar','baz'];
    var result={};
    for (var i=0; i<outputs.length; i++) {
      var output = outputs[i];
      if (output in j) result[output]=j[output];
    }
    return result;
  }
