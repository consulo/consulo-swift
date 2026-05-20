/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.swift.language.impl.syntax;

import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.lexer.LexerBase;
import consulo.swift.language.psi.SwiftTypes;

%%

%public
%class _SwiftLexer
%extends LexerBase
%function advanceImpl
%type IElementType
%unicode
%eof{  return;
%eof}

%{
  // Block-comment nesting state.
  private int blockCommentDepth = 0;

  // Whitespace-sensitive operator classification (Swift lexical-structure rules).
  // "leftHasWS" is true iff the character to the left of the *next* token is
  // considered whitespace per Swift's lexical rules. Updated after every emit.
  private boolean leftHasWS = true;

  private boolean rightHasWS() {
    if (zzMarkedPos >= zzEndRead) return true;
    char c = zzBuffer.charAt(zzMarkedPos);
    return c == ' ' || c == '\t' || c == '\n' || c == '\r'
        || c == ')' || c == ']' || c == '}'
        || c == ',' || c == ';' || c == ':';
  }

  private boolean rightIsDot() {
    return zzMarkedPos < zzEndRead && zzBuffer.charAt(zzMarkedPos) == '.';
  }

  private IElementType classifyOp() {
    boolean wsLeft  = leftHasWS;
    boolean wsRight = rightHasWS();
    boolean dotRight = rightIsDot();
    String text = yytext().toString();
    IElementType result;
    if ((text.equals("!") || text.equals("?")) && !wsLeft) {
      result = SwiftTypes.POSTFIX_OP;
    } else if (!wsLeft && dotRight) {
      result = SwiftTypes.POSTFIX_OP;
    } else if (wsLeft == wsRight) {
      result = SwiftTypes.BINARY_OP;
    } else if (wsLeft) {
      result = SwiftTypes.PREFIX_OP;
    } else {
      result = SwiftTypes.POSTFIX_OP;
    }
    leftHasWS = false;
    return result;
  }

  // Mark current emit as a "term" — operator immediately to its right has no
  // whitespace on its left.
  private IElementType term(IElementType t) { leftHasWS = false; return t; }

  // Mark current emit as a "left-WS-equivalent" — operator immediately to its
  // right is treated as if it has whitespace on the left. Per Swift docs:
  // '(', '[', '{' before an operator + ',', ';', ':' all count as left-WS.
  private IElementType leftWS(IElementType t) { leftHasWS = true; return t; }
%}

%state IN_BLOCK_COMMENT

WHITE_SPACE        = [ \t\n\r\u000B\u000C\u0000]+

LINE_COMMENT       = "//" [^\r\n]*

ID_HEAD            = [a-zA-Z_]
ID_CHAR            = [a-zA-Z_0-9]
IDENTIFIER         = {ID_HEAD} {ID_CHAR}*
BACKTICK_IDENT     = "`" {ID_HEAD} {ID_CHAR}* "`"
IMPLICIT_PARAM     = "$" [0-9]+
// Property-wrapper projected-value (e.g. `$showDetails` in SwiftUI). Syntactically
// behaves like an identifier expression.
DOLLAR_IDENT       = "$" {ID_HEAD} {ID_CHAR}*

PURE_DEC           = [0-9]+
DEC_LIT            = [0-9] [0-9_]*
BIN_LIT            = "0b" [01] [01_]*
OCT_LIT            = "0o" [0-7] [0-7_]*
HEX_LIT            = "0x" [0-9a-fA-F] [0-9a-fA-F_]*

DEC_FRAC           = "." {DEC_LIT}
DEC_EXP            = [eE] [+\-]? {DEC_LIT}
HEX_FRAC           = "." [0-9a-fA-F] [0-9a-fA-F_]*
HEX_EXP            = [pP] [+\-]? {DEC_LIT}
// Float literal must have a fraction or exponent — otherwise it would
// subsume plain integer DEC_LIT.
FLOAT_LIT          = {DEC_LIT} {DEC_FRAC} {DEC_EXP}?
                   | {DEC_LIT} {DEC_EXP}
                   | {HEX_LIT} {HEX_FRAC}? {HEX_EXP}

ESC_CHAR           = "\\" ( [0\\tnr\"'] | "x" [0-9a-fA-F]{2} | "u" "{" [0-9a-fA-F]{1,8} "}" )
// `\(...)` interpolation. JFlex regex can't recursively match balanced parens,
// so we hand-unroll one level of nesting plus one level of nested strings —
// enough for `\(foo(x))`, `\(type(of: y))`, and `\(label: "x")`. Deeper
// nesting needs a lexer state machine.
INTERP_INNER_CHAR  = {ESC_CHAR} | [^()\"\n\r\\]
NESTED_STR_ITEM    = {ESC_CHAR} | [^\"\n\r\\]
NESTED_STR         = "\"" {NESTED_STR_ITEM}* "\""
INTERP_INNER       = {INTERP_INNER_CHAR} | {NESTED_STR}
INTERP_SEG         = "\\(" {INTERP_INNER}* ( "(" {INTERP_INNER}* ")" {INTERP_INNER}* )* ")"
QUOTED_ITEM        = {ESC_CHAR} | {INTERP_SEG} | [^\"\n\r\\]
STATIC_STR         = "\"" {QUOTED_ITEM}* "\""

// Swift operator-head ASCII portion. Defined as an alternation to avoid
// JFlex character-class parsing quirks around '?'.
OP_CHAR            = ("/" | "=" | "-" | "+" | "!" | "*" | "%" | "&" | "|" | "<" | ">" | "^" | "~" | "?")

%%

<YYINITIAL> {
  {WHITE_SPACE}            { leftHasWS = true; return TokenType.WHITE_SPACE; }
  {LINE_COMMENT}           { leftHasWS = true; return SwiftTypes.LINE_COMMENT; }
  "/*"                     { yybegin(IN_BLOCK_COMMENT); blockCommentDepth = 1; }

  // ---- Compiler directives (must precede '#' fallback) ----
  "#if"                    { return term(SwiftTypes.HASH_IF); }
  "#elseif"                { return term(SwiftTypes.HASH_ELSEIF); }
  "#else"                  { return term(SwiftTypes.HASH_ELSE); }
  "#endif"                 { return term(SwiftTypes.HASH_ENDIF); }
  "#sourceLocation"        { return term(SwiftTypes.HASH_SOURCELOCATION); }
  "#available"             { return term(SwiftTypes.HASH_AVAILABLE); }
  "#selector"              { return term(SwiftTypes.HASH_SELECTOR); }
  "#keyPath"               { return term(SwiftTypes.HASH_KEYPATH); }
  "#colorLiteral"          { return term(SwiftTypes.HASH_COLOR_LITERAL); }
  "#fileLiteral"           { return term(SwiftTypes.HASH_FILE_LITERAL); }
  "#imageLiteral"          { return term(SwiftTypes.HASH_IMAGE_LITERAL); }
  "#file"                  { return term(SwiftTypes.HASH_FILE); }
  "#line"                  { return term(SwiftTypes.HASH_LINE); }
  "#column"                { return term(SwiftTypes.HASH_COLUMN); }
  "#function"              { return term(SwiftTypes.HASH_FUNCTION); }
  "#dsohandle"             { return term(SwiftTypes.HASH_DSOHANDLE); }

  // ---- Declaration keywords ----
  "import"                 { return term(SwiftTypes.IMPORT_KEYWORD); }
  "let"                    { return term(SwiftTypes.LET_KEYWORD); }
  "var"                    { return term(SwiftTypes.VAR_KEYWORD); }
  "typealias"              { return term(SwiftTypes.TYPEALIAS_KEYWORD); }
  "func"                   { return term(SwiftTypes.FUNC_KEYWORD); }
  "enum"                   { return term(SwiftTypes.ENUM_KEYWORD); }
  "struct"                 { return term(SwiftTypes.STRUCT_KEYWORD); }
  "class"                  { return term(SwiftTypes.CLASS_KEYWORD); }
  "protocol"               { return term(SwiftTypes.PROTOCOL_KEYWORD); }
  "init"                   { return term(SwiftTypes.INIT_KEYWORD); }
  "deinit"                 { return term(SwiftTypes.DEINIT_KEYWORD); }
  "extension"              { return term(SwiftTypes.EXTENSION_KEYWORD); }
  "subscript"              { return term(SwiftTypes.SUBSCRIPT_KEYWORD); }
  "operator"               { return term(SwiftTypes.OPERATOR_KEYWORD); }
  "precedencegroup"        { return term(SwiftTypes.PRECEDENCEGROUP_KEYWORD); }
  "associatedtype"         { return term(SwiftTypes.ASSOCIATEDTYPE_KEYWORD); }
  // `indirect` is contextual — see comment under hard-modifier section.

  // ---- Statement keywords ----
  "if"                     { return term(SwiftTypes.IF_KEYWORD); }
  "else"                   { return term(SwiftTypes.ELSE_KEYWORD); }
  "guard"                  { return term(SwiftTypes.GUARD_KEYWORD); }
  "switch"                 { return term(SwiftTypes.SWITCH_KEYWORD); }
  "case"                   { return term(SwiftTypes.CASE_KEYWORD); }
  "default"                { return term(SwiftTypes.DEFAULT_KEYWORD); }
  "for"                    { return term(SwiftTypes.FOR_KEYWORD); }
  "in"                     { return term(SwiftTypes.IN_KEYWORD); }
  "while"                  { return term(SwiftTypes.WHILE_KEYWORD); }
  "repeat"                 { return term(SwiftTypes.REPEAT_KEYWORD); }
  "do"                     { return term(SwiftTypes.DO_KEYWORD); }
  "defer"                  { return term(SwiftTypes.DEFER_KEYWORD); }
  "break"                  { return term(SwiftTypes.BREAK_KEYWORD); }
  "continue"               { return term(SwiftTypes.CONTINUE_KEYWORD); }
  "fallthrough"            { return term(SwiftTypes.FALLTHROUGH_KEYWORD); }
  "return"                 { return term(SwiftTypes.RETURN_KEYWORD); }
  "throw"                  { return term(SwiftTypes.THROW_KEYWORD); }
  "try"                    { return term(SwiftTypes.TRY_KEYWORD); }
  "catch"                  { return term(SwiftTypes.CATCH_KEYWORD); }
  "where"                  { return term(SwiftTypes.WHERE_KEYWORD); }
  "as"                     { return term(SwiftTypes.AS_KEYWORD); }
  "is"                     { return term(SwiftTypes.IS_KEYWORD); }

  // ---- Expression keywords ----
  "self"                   { return term(SwiftTypes.SELF_KEYWORD); }
  "Self"                   { return term(SwiftTypes.SELF_TYPE_KEYWORD); }
  "super"                  { return term(SwiftTypes.SUPER_KEYWORD); }
  "nil"                    { return term(SwiftTypes.NIL_KEYWORD); }
  "true"                   { return term(SwiftTypes.TRUE_KEYWORD); }
  "false"                  { return term(SwiftTypes.FALSE_KEYWORD); }
  "throws"                 { return term(SwiftTypes.THROWS_KEYWORD); }
  "rethrows"               { return term(SwiftTypes.RETHROWS_KEYWORD); }
  "inout"                  { return term(SwiftTypes.INOUT_KEYWORD); }
  "Any"                    { return term(SwiftTypes.ANY_KEYWORD); }
  "Type"                   { return term(SwiftTypes.TYPE_TYPE_KEYWORD); }
  "Protocol"               { return term(SwiftTypes.PROTOCOL_TYPE_KEYWORD); }
  // 'some' is a Swift 5.1+ contextual keyword — lexed as IDENTIFIER (below)
  // and remapped to SOME_KEYWORD by SwiftParserUtil only in type contexts.

  // ---- Modifiers ----
  // ---- Hard modifier keywords (access level + static) ----
  "private"                { return term(SwiftTypes.PRIVATE_KEYWORD); }
  "fileprivate"            { return term(SwiftTypes.FILEPRIVATE_KEYWORD); }
  "internal"               { return term(SwiftTypes.INTERNAL_KEYWORD); }
  "public"                 { return term(SwiftTypes.PUBLIC_KEYWORD); }
  "open"                   { return term(SwiftTypes.OPEN_KEYWORD); }
  "static"                 { return term(SwiftTypes.STATIC_KEYWORD); }

  // Contextual keywords below are lexed as IDENTIFIER and remapped by
  // SwiftParserUtil only when the parser expects the specific keyword token:
  //   final, lazy, weak, unowned, mutating, nonmutating, override, required,
  //   optional, dynamic, convenience, prefix, infix, postfix, indirect,
  //   get, set, willSet, didSet,
  //   left, right, none, associativity, higherThan, lowerThan, assignment.
  // This lets the same words be used as identifiers in non-modifier positions
  // (e.g. `let final = 5`, `func lazy() {}`).

  // ---- Punctuation that is "left-WS-equivalent" for next operator ----
  "{"                      { return leftWS(SwiftTypes.LBRACE); }
  "("                      { return leftWS(SwiftTypes.LPAREN); }
  "["                      { return leftWS(SwiftTypes.LBRACK); }
  ","                      { return leftWS(SwiftTypes.COMMA); }
  ";"                      { return leftWS(SwiftTypes.SEMI); }
  ":"                      { return leftWS(SwiftTypes.COLON); }

  // ---- Close brackets are "term-like" ----
  "}"                      { return term(SwiftTypes.RBRACE); }
  ")"                      { return term(SwiftTypes.RPAREN); }
  "]"                      { return term(SwiftTypes.RBRACK); }

  "@"                      { return term(SwiftTypes.AT); }
  "`"                      { return term(SwiftTypes.BACKTICK); }

  // ---- Range / arrow / multi-char forms (longest match) ----
  "..."                    { return term(SwiftTypes.ELLIPSIS); }
  "..<"                    { return term(SwiftTypes.HALF_OPEN_RANGE); }
  "..="                    { return term(SwiftTypes.DOUBLE_DOT_EQ); }
  "->"                     { return term(SwiftTypes.ARROW); }

  // ---- Single-character reserved operators ----
  "="                      { return term(SwiftTypes.EQ); }
  "<"                      { return term(SwiftTypes.LT); }
  ">"                      { return term(SwiftTypes.GT); }
  "?"                      { return term(SwiftTypes.QUESTION); }
  "!"                      { return term(SwiftTypes.EXCL); }
  "&"                      { return term(SwiftTypes.AMP); }
  "_"                      { return term(SwiftTypes.UNDERSCORE); }

  // ---- Literals ----
  {STATIC_STR}             { return term(SwiftTypes.STATIC_STRING_LITERAL); }
  {FLOAT_LIT}              { return term(SwiftTypes.FLOATING_POINT_LITERAL); }
  {BIN_LIT}                { return term(SwiftTypes.BINARY_LITERAL); }
  {OCT_LIT}                { return term(SwiftTypes.OCTAL_LITERAL); }
  {HEX_LIT}                { return term(SwiftTypes.HEXADECIMAL_LITERAL); }
  {DEC_LIT}                { return term(SwiftTypes.DECIMAL_LITERAL); }
  {PURE_DEC}               { return term(SwiftTypes.PURE_DECIMAL_DIGITS); }

  // ---- Identifiers ----
  {BACKTICK_IDENT}         { return term(SwiftTypes.IDENTIFIER); }
  {IMPLICIT_PARAM}         { return term(SwiftTypes.IMPLICIT_PARAMETER_NAME); }
  {DOLLAR_IDENT}           { return term(SwiftTypes.IMPLICIT_PARAMETER_NAME); }
  {IDENTIFIER}             { return term(SwiftTypes.IDENTIFIER); }

  // ---- Operator catch-all (classified by whitespace context) ----
  "." {OP_CHAR}+           { classifyOp(); return SwiftTypes.DOT_OPERATOR; }
  "."                      { return term(SwiftTypes.DOT); }
  {OP_CHAR}+               { return classifyOp(); }

  "#"                      { leftHasWS = false; return TokenType.BAD_CHARACTER; }
}

<IN_BLOCK_COMMENT> {
  "/*"                     { blockCommentDepth++; }
  "*/"                     {
                              blockCommentDepth--;
                              if (blockCommentDepth == 0) {
                                  yybegin(YYINITIAL);
                                  leftHasWS = true;
                                  return SwiftTypes.BLOCK_COMMENT;
                              }
                           }
  [^/*]+                   { /* swallow */ }
  [/*]                     { /* swallow stray */ }
  <<EOF>>                  { yybegin(YYINITIAL); leftHasWS = true; return SwiftTypes.BLOCK_COMMENT; }
}

[^]                        { leftHasWS = false; return TokenType.BAD_CHARACTER; }
