# Makefile.am                                          -*-Makefile-*-

include $(top_srcdir)/config/Transformers.mk
-include notemplate.c.dep
-include eval-Cxx.c.dep
-include deletecons.c.dep

PGEN_FLAGS      = -m $*
SDF2RTG_FLAGS	= -m $*
SCFLAGS		= --verbose 0 -O 2
STRINCLUDES	= -I $(SDF_ATTRIBUTE)/share/sdf-attribute	\
		  -I $(srcdir)					\
		  -I $(STR_LAZY)/share/str-lazy

noinst_PROGRAMS = notemplate deletecons
libexec_PROGRAMS = eval-Cxx

ATTRSDF2TABLE_FLAGS = --imp CxxLib
AG_GRM 		= Cxx.ag

pkgdata_DATA	= \
		  Cxx.edef.af \
		  Cxx.edef \
		  Cxx.def.af \
		  Cxx.def \
		  Cxx.tbl \
		  Cxx.pp.af \
		  Cxx.pp \
		  Cxx.str \
		  Cxx.rtree

EXTRA_DIST	= $(SDFS) 			\
		notemplate.str 			\
		deletecons.str			\
		CxxLib.str			\
		ns-lookup.str			\
		ExtraCons.str			\
		Ids.str				\
		codeboost-tree.str		\
		valid-types.str			\
		kinds.str

SDFS		= \
		AccessSpecifiers.sdf \
		AdditiveExpressions.sdf \
		AndExpressions.sdf \
		AsmDefinitions.sdf \
		AssignmentExpressions.sdf \
		BaseSpecifiers.sdf \
		BooleanLiterals.sdf \
		CastExpressions.sdf \
		CharacterLiterals.sdf \
		Characters.sdf \
		ClassSpecifiers.sdf \
		CompoundStatements.sdf \
		ConditionalExpressions.sdf \
		ConstantExpressions.sdf \
		ConversionFunctionIds.sdf \
		Cxx.sdf \
		CtorInitializers.sdf \
		CvQualifiers.sdf \
		DeclarationStatements.sdf \
		Declarations.sdf \
		Declarators.sdf \
		DeleteExpressions.sdf \
		ElaboratedTypeSpecifiers.sdf \
		EnumSpecifiers.sdf \
		EqualityExpressions.sdf \
		EscapeSequences.sdf \
		ExceptionSpecifications.sdf \
		ExclusiveOrExpressions.sdf \
		ExplicitInstantiations.sdf \
		ExplicitSpecializations.sdf \
		ExpressionStatements.sdf \
		Expressions.sdf \
		FloatingLiterals.sdf \
		FunctionDefinitions.sdf \
		FunctionSpecifiers.sdf \
		IdExpressions.sdf \
		Identifiers.sdf \
		InclusiveOrExpressions.sdf \
		InitDeclarators.sdf \
		Initializers.sdf \
		IntegerLiterals.sdf \
		IterationStatements.sdf \
		JumpStatements.sdf \
		Keywords.sdf \
		LabeledStatements.sdf \
		Layout.sdf \
		LinkageSpecifications.sdf \
		Literals.sdf \
		LogicalAndExpressions.sdf \
		LogicalOrExpressions.sdf \
		MemberSpecifications.sdf \
		MultiplicativeExpressions.sdf \
		NamespaceAliases.sdf \
		Namespaces.sdf \
		NewExpressions.sdf \
		OperatorFunctionIds.sdf \
		ParameterDeclarations.sdf \
		PmExpressions.sdf \
		PostfixExpressions.sdf \
		PrimaryExpressions.sdf \
		RelationalExpressions.sdf \
		SelectionStatements.sdf \
		ShiftExpressions.sdf \
		SimpleTypeSpecifiers.sdf \
		Specifiers.sdf \
		Statements.sdf \
		StorageClassSpecifiers.sdf \
		StringLiterals.sdf \
		TemplateDeclarations.sdf \
		TemplateIds.sdf \
		TemplateParameters.sdf \
		ThrowExpressions.sdf \
		TranslationUnits.sdf \
		TryBlocks.sdf \
		TypeIds.sdf \
		TypeSpecifiers.sdf \
		UnaryExpressions.sdf \
		UsingDeclarations.sdf \
		UsingDirectives.sdf

Cxx.edef.af: notemplate deletecons Cxx.edef-fast
	$(ESDF)/bin/pack-esdf --verbose 1 \
		-of ast \
		-s AttrSdf \
		-I $(SDF_ATTRIBUTE)/share/sdf-attribute \
		-Idef Cxx.edef-fast \
		-i $(srcdir)/Cxx.sdf \
	| $(SDF_OPTION)/bin/sdf-option -b \
	| $(SDF_TOOLS)/libexec/sdf-cons -b \
	| ./deletecons -b \
	| $(BOXEDSDF)/libexec/boxed-desugar -b \
	| ./notemplate -b -o $@

nodist_notemplate_SOURCES  = notemplate.c
nodist_deletecons_SOURCES  = deletecons.c
CLEANFILES = $(pkgdata_DATA) notemplate.{c.dep,c} eval-Cxx.{c,str,c.dep} \
	     deletecons.{c.dep,c} Cxx.edef-fast

LDADD += $(SSL_LIBS) $(STR_LAZY_LIBS)

eval_Cxx_LDFLAGS  = -R $(DIST_STR_LAZY)
eval_Cxx_CFLAGS	  = $(CFLAGS_EVAL)
