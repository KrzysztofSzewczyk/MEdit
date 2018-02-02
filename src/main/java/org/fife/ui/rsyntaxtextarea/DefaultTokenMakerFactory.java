/*
 * 12/14/2008
 *
 * DefaultTokenMakerFactory.java - The default TokenMaker factory.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

/**
 * The default implementation of <code>TokenMakerFactory</code>. This factory
 * can create {@link TokenMaker}s for all languages known to
 * {@link RSyntaxTextArea}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class DefaultTokenMakerFactory extends AbstractTokenMakerFactory implements SyntaxConstants {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initTokenMakerMap() {

		final String pkg = "org.fife.ui.rsyntaxtextarea.modes.";

		this.putMapping(SyntaxConstants.SYNTAX_STYLE_NONE, pkg + "PlainTextTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT, pkg + "ActionScriptTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86, pkg + "AssemblerX86TokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_BBCODE, pkg + "BBCodeTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_C, pkg + "CTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_CLOJURE, pkg + "ClojureTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, pkg + "CPlusPlusTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_CSHARP, pkg + "CSharpTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_CSS, pkg + "CSSTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_D, pkg + "DTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_DART, pkg + "DartTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_DELPHI, pkg + "DelphiTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_DOCKERFILE, pkg + "DockerTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_DTD, pkg + "DtdTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_FORTRAN, pkg + "FortranTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_GROOVY, pkg + "GroovyTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_HOSTS, pkg + "HostsTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_HTACCESS, pkg + "HtaccessTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_HTML, pkg + "HTMLTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_INI, pkg + "IniTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_JAVA, pkg + "JavaTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, pkg + "JavaScriptTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS, pkg + "JshintrcTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_JSON, pkg + "JsonTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_JSP, pkg + "JSPTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_LATEX, pkg + "LatexTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_LESS, pkg + "LessTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_LISP, pkg + "LispTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_LUA, pkg + "LuaTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_MAKEFILE, pkg + "MakefileTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_MXML, pkg + "MxmlTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_NSIS, pkg + "NSISTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_PERL, pkg + "PerlTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_PHP, pkg + "PHPTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE, pkg + "PropertiesFileTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_PYTHON, pkg + "PythonTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_RUBY, pkg + "RubyTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_SAS, pkg + "SASTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_SCALA, pkg + "ScalaTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_SQL, pkg + "SQLTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_TCL, pkg + "TclTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT, pkg + "TypeScriptTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL, pkg + "UnixShellTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC, pkg + "VisualBasicTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH, pkg + "WindowsBatchTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_XML, pkg + "XMLTokenMaker");
		this.putMapping(SyntaxConstants.SYNTAX_STYLE_YAML, pkg + "YamlTokenMaker");

	}

}