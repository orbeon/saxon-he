<?xml version="1.0" encoding="UTF-8"?>
<scm:schema xmlns:scm="http://ns.saxonica.com/schema-component-model"
            generatedAt="2018-03-14T10:05:56.077Z"
            xsdVersion="1.1">
   <scm:simpleType id="C0"
                   name="finalType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C1"/>
   <scm:simpleType id="C2"
                   name="xsdVersionType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="1.0"/>
      <scm:enumeration value="1.1"/>
   </scm:simpleType>
   <scm:simpleType id="C3"
                   name="processContentsType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="lax"/>
      <scm:enumeration value="skip"/>
      <scm:enumeration value="strict"/>
   </scm:simpleType>
   <scm:simpleType id="C4"
                   name="whitespaceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="preserve"/>
      <scm:enumeration value="replace"/>
      <scm:enumeration value="collapse"/>
   </scm:simpleType>
   <scm:simpleType id="C5"
                   name="pseudoQNameType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="##defined"/>
      <scm:enumeration value="##definedSibling"/>
   </scm:simpleType>
   <scm:simpleType id="C6"
                   name="openContentModeType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="interleave"/>
      <scm:enumeration value="suffix"/>
   </scm:simpleType>
   <scm:simpleType id="C7"
                   name="xpathExpressionType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value=".+"/>
   </scm:simpleType>
   <scm:simpleType id="C8"
                   name="typeReferenceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="C9 #IDREF"/>
   <scm:simpleType id="C10"
                   name="_langType"
                   targetNamespace="http://www.w3.org/XML/1998/namespace"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="#language C11"/>
   <scm:simpleType id="C12"
                   name="typeReferenceListType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C8"/>
   <scm:complexType id="C13"
                    name="identityConstraintType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C14"/>
      <scm:attributeUse required="true" inheritable="false" ref="C15"/>
      <scm:attributeUse required="false" inheritable="false" ref="C16"/>
      <scm:attributeUse required="false" inheritable="false" ref="C17"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C18"/>
            <scm:elementParticle minOccurs="1" maxOccurs="unbounded" ref="C19"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C18" to="1"/>
         </scm:state>
         <scm:state nr="1">
            <scm:edge term="C19" to="2"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C19" to="3"/>
         </scm:state>
         <scm:state nr="3" final="true">
            <scm:edge term="C19" to="3"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:simpleType id="C20"
                   name="pseudoNamespaceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="##absent"/>
      <scm:enumeration value="##other"/>
      <scm:enumeration value="##any"/>
      <scm:enumeration value="##targetNamespace"/>
      <scm:enumeration value="##defaultNamespace"/>
      <scm:enumeration value="##local"/>
   </scm:simpleType>
   <scm:simpleType id="C21"
                   name="namespaceListType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C22"/>
   <scm:simpleType id="C22"
                   name="namespaceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="C20 C23"/>
   <scm:simpleType id="C9"
                   name="builtInTypeReferenceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value="#[a-zA-Z0-9]+\*?"/>
   </scm:simpleType>
   <scm:simpleType id="C24"
                   name="notQNameListType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C25"/>
   <scm:complexType id="C26"
                    name="xpathContainerType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C27"/>
      <scm:attributeUse required="true" inheritable="false" ref="C28"/>
      <scm:attributeUse required="false" inheritable="false" ref="C29"/>
      <scm:attributeUse required="false" inheritable="false" ref="C30"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C31"
                    name="abstractParticleType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="true"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C32"/>
      <scm:attributeUse required="true" inheritable="false" ref="C33"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:simpleType id="C23"
                   name="uriType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#token"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value="[^\s\r\n\t]*"/>
   </scm:simpleType>
   <scm:complexType id="C34"
                    name="typedValueType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C35"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C36"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C36" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C36" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:simpleType id="C37"
                   name="complexVarietyType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="simple"/>
      <scm:enumeration value="element-only"/>
      <scm:enumeration value="empty"/>
      <scm:enumeration value="mixed"/>
   </scm:simpleType>
   <scm:simpleType id="C38"
                   name="maxOccursType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="#nonNegativeInteger C39"/>
   <scm:simpleType id="C11"
                   name="zero-length-string"
                   targetNamespace="http://saxon.sf.net/"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:length value="0"/>
   </scm:simpleType>
   <scm:simpleType id="C40"
                   name="blockType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C1"/>
   <scm:simpleType id="C41"
                   name="explicitTimezoneType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="prohibited"/>
      <scm:enumeration value="optional"/>
      <scm:enumeration value="required"/>
   </scm:simpleType>
   <scm:simpleType id="C1"
                   name="derivationMethodType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="restriction"/>
      <scm:enumeration value="substitution"/>
      <scm:enumeration value="list"/>
      <scm:enumeration value="extension"/>
      <scm:enumeration value="union"/>
   </scm:simpleType>
   <scm:simpleType id="C25"
                   name="notQNameType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="C5 C42 #NCName"/>
   <scm:simpleType id="C42"
                   name="clarkNameType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value="\{[^{}]*\}\i\c*"/>
   </scm:simpleType>
   <scm:simpleType id="C39"
                   name="unboundedType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="unbounded"/>
   </scm:simpleType>
   <scm:simpleType id="C43"
                   name="_spaceType"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="preserve"/>
      <scm:enumeration value="default"/>
   </scm:simpleType>
   <scm:element id="C44"
                name="simpleType"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C45"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C46"
                name="minExclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C47"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C49"
                name="wildcard"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C50"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C51"
                name="attribute"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C52"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C53"
                name="abstractParticle"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C31"
                global="true"
                nillable="false"
                abstract="true"/>
   <scm:element id="C54"
                name="substitutionGroupAffiliation"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C55"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C56"
                name="element"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C57"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C58"
                name="assert"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C59"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C60"
                name="maxScale"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C61"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C18"
                name="selector"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C26"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C62"
                name="state"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C63"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C19"
                name="field"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C26"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C64"
                name="minScale"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C65"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C66"
                name="maxExclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C67"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C68"
                name="explicitTimezone"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C69"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C70"
                name="elementParticle"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C71"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C53"/>
   </scm:element>
   <scm:element id="C72"
                name="totalDigits"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C73"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C74"
                name="whiteSpace"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C75"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C76"
                name="minInclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C77"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C78"
                name="alternativeType"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C79"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C80"
                name="complexType"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C81"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C82"
                name="attributeUse"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C83"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C84"
                name="finiteStateMachine"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C85"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C86"
                name="edge"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C87"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C48"
                name="abstractFacet"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="#anyType"
                global="true"
                nillable="false"
                abstract="true"/>
   <scm:element id="C88"
                name="unique"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C13"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C89"
                name="maxInclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C90"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C91"
                name="elementWildcard"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C92"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C53"/>
   </scm:element>
   <scm:element id="C93"
                name="modelGroupParticle"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C94"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C53"/>
   </scm:element>
   <scm:element id="C95"
                name="attributeWildcard"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C96"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C97"
                name="minLength"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C98"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C99"
                name="preprocess"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C100"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C101"
                name="attributeGroup"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C102"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C103"
                name="identityConstraint"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C104"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C105"
                name="choice"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C106"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C107"/>
   </scm:element>
   <scm:element id="C107"
                name="abstractModelGroup"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C106"
                global="true"
                nillable="false"
                abstract="true"/>
   <scm:element id="C108"
                name="fractionDigits"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C109"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C110"
                name="notation"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C111"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C112"
                name="maxLength"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C113"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C114"
                name="pattern"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C115"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C116"
                name="sequence"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C106"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C107"/>
   </scm:element>
   <scm:element id="C117"
                name="fixed"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C34"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C118"
                name="key"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C13"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C119"
                name="assertion"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C120"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C121"
                name="enumeration"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C122"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C123"
                name="modelGroupDefinition"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C124"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C125"
                name="openContent"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C126"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C127"
                name="keyref"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C13"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C128"
                name="schema"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C129"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C130"
                name="length"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C131"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C48"/>
   </scm:element>
   <scm:element id="C132"
                name="all"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C106"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C107"/>
   </scm:element>
   <scm:attribute id="C133"
                  name="lang"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="C10"
                  global="true"
                  inheritable="false"/>
   <scm:attribute id="C134"
                  name="id"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="#ID"
                  global="true"
                  inheritable="false"/>
   <scm:attribute id="C30"
                  name="base"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="#anyURI"
                  global="true"
                  inheritable="false"/>
   <scm:attribute id="C135"
                  name="space"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="C43"
                  global="true"
                  inheritable="false"/>
   <scm:complexType id="C122"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C136"/>
      <scm:attributeUse required="false" inheritable="false" ref="C137" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C94"
                    base="C31"
                    derivationMethod="extension"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C138"/>
      <scm:attributeUse required="true" inheritable="false" ref="C32"/>
      <scm:attributeUse required="true" inheritable="false" ref="C33"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C107"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C105" to="1"/>
            <scm:edge term="C132" to="1"/>
            <scm:edge term="C116" to="1"/>
            <scm:edge term="C107" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C105" to="1"/>
            <scm:edge term="C132" to="1"/>
            <scm:edge term="C116" to="1"/>
            <scm:edge term="C107" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C126"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="false" inheritable="false" ref="C139"/>
      <scm:attributeUse required="false" inheritable="false" ref="C140"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C136"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C122"/>
   <scm:complexType id="C83"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C141"/>
      <scm:attributeUse required="true" inheritable="false" ref="C142"/>
      <scm:attributeUse required="true" inheritable="false" ref="C143"/>
      <scm:attributeUse required="true" inheritable="false" ref="C144"/>
      <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C117"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C117" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C90"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C145"/>
      <scm:attributeUse required="false" inheritable="false" ref="C146" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C141"
                  name="default"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C83"/>
   <scm:attribute id="C140"
                  name="wildcard"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C126"/>
   <scm:complexType id="C131"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C147"/>
      <scm:attributeUse required="false" inheritable="false" ref="C148" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C147"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C131"/>
   <scm:complexType id="C71"
                    base="C31"
                    derivationMethod="extension"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C149"/>
      <scm:attributeUse required="true" inheritable="false" ref="C32"/>
      <scm:attributeUse required="true" inheritable="false" ref="C33"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C149"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C71"/>
   <scm:attribute id="C138"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C94"/>
   <scm:complexType id="C52"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C150"/>
      <scm:attributeUse required="false" inheritable="false" ref="C151"/>
      <scm:attributeUse required="true" inheritable="false" ref="C152"/>
      <scm:attributeUse required="true" inheritable="false" ref="C153"/>
      <scm:attributeUse required="true" inheritable="false" ref="C154"/>
      <scm:attributeUse required="false" inheritable="false" ref="C155"/>
      <scm:attributeUse required="true" inheritable="false" ref="C156"/>
      <scm:attributeUse required="true" inheritable="false" ref="C157"/>
      <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C117"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C117" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C154"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:attribute id="C153"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:attribute id="C150"
                  name="containingComplexType"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:complexType id="C100"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C158" default="false"/>
      <scm:elementParticle minOccurs="1" maxOccurs="2" ref="C119"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C119" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true" minOccurs="1" maxOccurs="2">
            <scm:edge term="C119" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C57"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C159"/>
      <scm:attributeUse required="false" inheritable="false" ref="C160"/>
      <scm:attributeUse required="false" inheritable="false" ref="C161"/>
      <scm:attributeUse required="false" inheritable="false" ref="C162"/>
      <scm:attributeUse required="false" inheritable="false" ref="C163"/>
      <scm:attributeUse required="true" inheritable="false" ref="C164"/>
      <scm:attributeUse required="true" inheritable="false" ref="C165"/>
      <scm:attributeUse required="true" inheritable="false" ref="C166"/>
      <scm:attributeUse required="true" inheritable="false" ref="C167"/>
      <scm:attributeUse required="false" inheritable="false" ref="C168"/>
      <scm:attributeUse required="true" inheritable="false" ref="C169"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C54"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C78"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C103"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C117"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C54" to="1"/>
            <scm:edge term="C103" to="2"/>
            <scm:edge term="C117" to="3"/>
            <scm:edge term="C78" to="4"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C54" to="1"/>
            <scm:edge term="C103" to="2"/>
            <scm:edge term="C117" to="3"/>
            <scm:edge term="C78" to="4"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C103" to="2"/>
            <scm:edge term="C117" to="3"/>
         </scm:state>
         <scm:state nr="3" final="true"/>
         <scm:state nr="4" final="true">
            <scm:edge term="C103" to="2"/>
            <scm:edge term="C117" to="3"/>
            <scm:edge term="C78" to="4"/>
         </scm:state>
      </scm:finiteStateMachine>
      <scm:assertion xmlns:xs="http://www.w3.org/2001/XMLSchema"
                     xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning"
                     test="not(@default and scm:fixed)"
                     defaultNamespace=""
                     xml:base="file:/Users/mike/repo2/samples/scm/scmschema.xsd"/>
   </scm:complexType>
   <scm:attribute id="C161"
                  name="containingComplexType"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C165"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C159"
                  name="abstract"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C160"
                  name="block"
                  type="C40"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C142"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C83"/>
   <scm:attribute id="C155"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:complexType id="C124"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C170"/>
      <scm:attributeUse required="false" inheritable="false" ref="C171"/>
      <scm:attributeUse required="false" inheritable="false" ref="C172"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C53"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C70" to="1"/>
            <scm:edge term="C53" to="1"/>
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C91" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C70" to="1"/>
            <scm:edge term="C53" to="1"/>
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C91" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C172"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C124"/>
   <scm:attribute id="C144"
                  name="inheritable"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C83"/>
   <scm:complexType id="C113"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C173"/>
      <scm:attributeUse required="false" inheritable="false" ref="C174" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C129"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C175"/>
      <scm:attributeUse required="false" inheritable="false" ref="C176"/>
      <scm:modelGroupParticle minOccurs="0" maxOccurs="unbounded">
         <scm:choice>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C56"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C51"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C80"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C44"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C101"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C123"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C110"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C49"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C88"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C118"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C127"/>
         </scm:choice>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C123" to="1"/>
            <scm:edge term="C51" to="1"/>
            <scm:edge term="C127" to="1"/>
            <scm:edge term="C80" to="1"/>
            <scm:edge term="C118" to="1"/>
            <scm:edge term="C110" to="1"/>
            <scm:edge term="C49" to="1"/>
            <scm:edge term="C56" to="1"/>
            <scm:edge term="C88" to="1"/>
            <scm:edge term="C44" to="1"/>
            <scm:edge term="C101" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C123" to="1"/>
            <scm:edge term="C51" to="1"/>
            <scm:edge term="C127" to="1"/>
            <scm:edge term="C80" to="1"/>
            <scm:edge term="C118" to="1"/>
            <scm:edge term="C110" to="1"/>
            <scm:edge term="C49" to="1"/>
            <scm:edge term="C56" to="1"/>
            <scm:edge term="C88" to="1"/>
            <scm:edge term="C44" to="1"/>
            <scm:edge term="C101" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C175"
                  name="generatedAt"
                  type="#dateTime"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C176"
                  name="xsdVersion"
                  type="C2"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C168"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C143"
                  name="required"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C83"/>
   <scm:attribute id="C27"
                  name="xpath"
                  type="C7"
                  global="false"
                  inheritable="false"
                  containingComplexType="C26"/>
   <scm:complexType id="C102"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C177"/>
      <scm:attributeUse required="false" inheritable="false" ref="C178"/>
      <scm:attributeUse required="false" inheritable="false" ref="C179"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C82"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C95"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C82" to="1"/>
            <scm:edge term="C95" to="2"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C82" to="1"/>
            <scm:edge term="C95" to="2"/>
         </scm:state>
         <scm:state nr="2" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C179"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C102"/>
   <scm:attribute id="C177"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C102"/>
   <scm:attribute id="C178"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C102"/>
   <scm:attribute id="C137"
                  name="namespaceSensitive"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C122"/>
   <scm:complexType id="C106"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:modelGroupParticle minOccurs="0" maxOccurs="unbounded">
         <scm:sequence>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C53"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C91" to="1"/>
            <scm:edge term="C70" to="1"/>
            <scm:edge term="C53" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C91" to="1"/>
            <scm:edge term="C70" to="1"/>
            <scm:edge term="C53" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C32"
                  name="maxOccurs"
                  type="C38"
                  global="false"
                  inheritable="false"
                  containingComplexType="C31"/>
   <scm:attribute id="C170"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C124"/>
   <scm:complexType id="C79"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C180"/>
      <scm:attributeUse required="true" inheritable="false" ref="C181"/>
      <scm:attributeUse required="true" inheritable="false" ref="C182"/>
      <scm:attributeUse required="false" inheritable="false" ref="C30"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C152"
                  name="global"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:complexType id="C104"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C183"/>
      <scm:attributeUse required="false" inheritable="false" ref="C30"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C183"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C104"/>
   <scm:attribute id="C35"
                  name="lexicalForm"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C34"/>
   <scm:complexType id="C61"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C184"/>
      <scm:attributeUse required="false" inheritable="false" ref="C185" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C185"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C61"/>
   <scm:attribute id="C145"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C90"/>
   <scm:complexType id="C63"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C186"/>
      <scm:attributeUse required="false" inheritable="false" ref="C187"/>
      <scm:attributeUse required="false" inheritable="false" ref="C188"/>
      <scm:attributeUse required="false" inheritable="false" ref="C189"/>
      <scm:attributeUse required="true" inheritable="false" ref="C190"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C86"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C86" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C86" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C187"
                  name="final"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C63"/>
   <scm:attribute id="C190"
                  name="nr"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C63"/>
   <scm:attribute id="C186"
                  name="afterMax"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C63"/>
   <scm:attribute id="C188"
                  name="maxOccurs"
                  type="C38"
                  global="false"
                  inheritable="false"
                  containingComplexType="C63"/>
   <scm:attribute id="C169"
                  name="type"
                  type="C8"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:complexType id="C120"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C191"/>
      <scm:attributeUse required="true" inheritable="false" ref="C192"/>
      <scm:attributeUse required="false" inheritable="false" ref="C30"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C191"
                  name="test"
                  type="C7"
                  global="false"
                  inheritable="false"
                  containingComplexType="C120"/>
   <scm:attribute id="C192"
                  name="defaultNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C120"/>
   <scm:complexType id="C115"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C193"/>
      <scm:attributeUse required="false" inheritable="false" ref="C194" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C193"
                  name="value"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C115"/>
   <scm:attribute id="C194"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C115"/>
   <scm:complexType id="C75"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C195"/>
      <scm:attributeUse required="false" inheritable="false" ref="C196" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C28"
                  name="defaultNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C26"/>
   <scm:attribute id="C167"
                  name="nillable"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C15"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C13"/>
   <scm:attribute id="C16"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C13"/>
   <scm:attribute id="C33"
                  name="minOccurs"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C31"/>
   <scm:element id="C36"
                name="item"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C197"
                global="false"
                containingComplexType="C34"
                nillable="false"
                abstract="false"/>
   <scm:complexType id="C197"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C198"/>
      <scm:attributeUse required="true" inheritable="false" ref="C199"/>
      <scm:attributeUse required="false" inheritable="false" ref="C200"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C199"
                  name="value"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C197"/>
   <scm:attribute id="C198"
                  name="type"
                  type="C9"
                  global="false"
                  inheritable="false"
                  containingComplexType="C197"/>
   <scm:complexType id="C85"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C201"/>
      <scm:elementParticle minOccurs="1" maxOccurs="unbounded" ref="C62"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C62" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C62" to="2"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C62" to="2"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C96"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C202"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C202"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C96"/>
   <scm:attribute id="C29"
                  name="type"
                  type="C9"
                  global="false"
                  inheritable="false"
                  containingComplexType="C26"/>
   <scm:complexType id="C67"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C203"/>
      <scm:attributeUse required="false" inheritable="false" ref="C204" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C166"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C173"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C113"/>
   <scm:complexType id="C87"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C205"/>
      <scm:attributeUse required="true" inheritable="false" ref="C206"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C205"
                  name="term"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C87"/>
   <scm:attribute id="C204"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C67"/>
   <scm:attribute id="C189"
                  name="minOccurs"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C63"/>
   <scm:attribute id="C156"
                  name="type"
                  type="C8"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:complexType id="C92"
                    base="C31"
                    derivationMethod="extension"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C207"/>
      <scm:attributeUse required="true" inheritable="false" ref="C32"/>
      <scm:attributeUse required="true" inheritable="false" ref="C33"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C148"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C131"/>
   <scm:attribute id="C182"
                  name="defaultNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C79"/>
   <scm:attribute id="C164"
                  name="global"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C157"
                  name="inheritable"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:complexType id="C98"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C208"/>
      <scm:attributeUse required="false" inheritable="false" ref="C209" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C209"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C98"/>
   <scm:attribute id="C208"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C98"/>
   <scm:attribute id="C162"
                  name="default"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:complexType id="C73"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C210"/>
      <scm:attributeUse required="false" inheritable="false" ref="C211" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C210"
                  name="value"
                  type="#positiveInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C73"/>
   <scm:attribute id="C211"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C73"/>
   <scm:attribute id="C200"
                  name="namespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C197"/>
   <scm:attribute id="C196"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C75"/>
   <scm:attribute id="C158"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C100"/>
   <scm:attribute id="C180"
                  name="type"
                  type="C8"
                  global="false"
                  inheritable="false"
                  containingComplexType="C79"/>
   <scm:complexType id="C109"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C212"/>
      <scm:attributeUse required="false" inheritable="false" ref="C213" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C213"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C109"/>
   <scm:attribute id="C212"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C109"/>
   <scm:attribute id="C14"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C13"/>
   <scm:attribute id="C171"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C124"/>
   <scm:complexType id="C69"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C214"/>
      <scm:attributeUse required="false" inheritable="false" ref="C215" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C214"
                  name="value"
                  type="C41"
                  global="false"
                  inheritable="false"
                  containingComplexType="C69"/>
   <scm:attribute id="C215"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C69"/>
   <scm:attribute id="C203"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C67"/>
   <scm:attribute id="C206"
                  name="to"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C87"/>
   <scm:attribute id="C195"
                  name="value"
                  type="C4"
                  global="false"
                  inheritable="false"
                  containingComplexType="C75"/>
   <scm:complexType id="C111"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C216"/>
      <scm:attributeUse required="false" inheritable="false" ref="C217"/>
      <scm:attributeUse required="false" inheritable="false" ref="C218"/>
      <scm:attributeUse required="false" inheritable="false" ref="C219"/>
      <scm:attributeUse required="false" inheritable="false" ref="C220"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C220"
                  name="publicId"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C111"/>
   <scm:attribute id="C218"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C111"/>
   <scm:attribute id="C216"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C111"/>
   <scm:attribute id="C219"
                  name="systemId"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C111"/>
   <scm:attribute id="C217"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C111"/>
   <scm:attribute id="C181"
                  name="test"
                  type="C7"
                  global="false"
                  inheritable="false"
                  containingComplexType="C79"/>
   <scm:complexType id="C47"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C221"/>
      <scm:attributeUse required="false" inheritable="false" ref="C222" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C222"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C47"/>
   <scm:attribute id="C221"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C47"/>
   <scm:attribute id="C151"
                  name="default"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C52"/>
   <scm:complexType id="C55"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C223"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C223"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:complexType id="C65"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C224"/>
      <scm:attributeUse required="false" inheritable="false" ref="C225" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C225"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C65"/>
   <scm:attribute id="C224"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C65"/>
   <scm:complexType id="C77"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C226"/>
      <scm:attributeUse required="false" inheritable="false" ref="C227" default="false"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C226"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C77"/>
   <scm:attribute id="C17"
                  name="key"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C13"/>
   <scm:attribute id="C163"
                  name="final"
                  type="C0"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C201"
                  name="initialState"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C85"/>
   <scm:attribute id="C139"
                  name="mode"
                  type="C6"
                  global="false"
                  inheritable="false"
                  containingComplexType="C126"/>
   <scm:complexType id="C81"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C228"/>
      <scm:attributeUse required="true" inheritable="false" ref="C229"/>
      <scm:attributeUse required="true" inheritable="false" ref="C230"/>
      <scm:attributeUse required="false" inheritable="false" ref="C231"/>
      <scm:attributeUse required="false" inheritable="false" ref="C232"/>
      <scm:attributeUse required="true" inheritable="false" ref="C233"/>
      <scm:attributeUse required="false" inheritable="false" ref="C234"/>
      <scm:attributeUse required="false" inheritable="false" ref="C235"/>
      <scm:attributeUse required="false" inheritable="false" ref="C236"/>
      <scm:attributeUse required="true" inheritable="false" ref="C237"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C125"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C82"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C95"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C53"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C84"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C119"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C53" to="1"/>
            <scm:edge term="C84" to="6"/>
            <scm:edge term="C95" to="2"/>
            <scm:edge term="C125" to="3"/>
            <scm:edge term="C91" to="1"/>
            <scm:edge term="C82" to="4"/>
            <scm:edge term="C119" to="5"/>
            <scm:edge term="C70" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C119" to="5"/>
            <scm:edge term="C84" to="6"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C91" to="1"/>
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C53" to="1"/>
            <scm:edge term="C119" to="5"/>
            <scm:edge term="C84" to="6"/>
            <scm:edge term="C70" to="1"/>
         </scm:state>
         <scm:state nr="3" final="true">
            <scm:edge term="C91" to="1"/>
            <scm:edge term="C95" to="2"/>
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C53" to="1"/>
            <scm:edge term="C82" to="4"/>
            <scm:edge term="C119" to="5"/>
            <scm:edge term="C84" to="6"/>
            <scm:edge term="C70" to="1"/>
         </scm:state>
         <scm:state nr="4" final="true">
            <scm:edge term="C91" to="1"/>
            <scm:edge term="C95" to="2"/>
            <scm:edge term="C93" to="1"/>
            <scm:edge term="C53" to="1"/>
            <scm:edge term="C82" to="4"/>
            <scm:edge term="C119" to="5"/>
            <scm:edge term="C84" to="6"/>
            <scm:edge term="C70" to="1"/>
         </scm:state>
         <scm:state nr="5" final="true">
            <scm:edge term="C119" to="5"/>
         </scm:state>
         <scm:state nr="6" final="true">
            <scm:edge term="C119" to="5"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C234"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C232"
                  name="final"
                  type="C0"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C237"
                  name="variety"
                  type="C37"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C230"
                  name="derivationMethod"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C235"
                  name="simpleType"
                  type="C8"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C228"
                  name="abstract"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C229"
                  name="base"
                  type="C8"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C236"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C227"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C77"/>
   <scm:attribute id="C231"
                  name="block"
                  type="C40"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C207"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C92"/>
   <scm:complexType id="C45"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C238"/>
      <scm:attributeUse required="false" inheritable="false" ref="C239"/>
      <scm:attributeUse required="true" inheritable="false" ref="C240"/>
      <scm:attributeUse required="false" inheritable="false" ref="C241"/>
      <scm:attributeUse required="false" inheritable="false" ref="C242"/>
      <scm:attributeUse required="false" inheritable="false" ref="C243"/>
      <scm:attributeUse required="false" inheritable="false" ref="C244"/>
      <scm:attributeUse required="false" inheritable="false" ref="C245"/>
      <scm:attributeUse required="true" inheritable="false" ref="C246"/>
      <scm:modelGroupParticle minOccurs="0" maxOccurs="unbounded">
         <scm:sequence>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C48"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C112" to="1"/>
            <scm:edge term="C72" to="1"/>
            <scm:edge term="C46" to="1"/>
            <scm:edge term="C48" to="1"/>
            <scm:edge term="C64" to="1"/>
            <scm:edge term="C68" to="1"/>
            <scm:edge term="C108" to="1"/>
            <scm:edge term="C66" to="1"/>
            <scm:edge term="C60" to="1"/>
            <scm:edge term="C114" to="1"/>
            <scm:edge term="C121" to="1"/>
            <scm:edge term="C130" to="1"/>
            <scm:edge term="C99" to="1"/>
            <scm:edge term="C76" to="1"/>
            <scm:edge term="C74" to="1"/>
            <scm:edge term="C97" to="1"/>
            <scm:edge term="C58" to="1"/>
            <scm:edge term="C89" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C112" to="1"/>
            <scm:edge term="C72" to="1"/>
            <scm:edge term="C46" to="1"/>
            <scm:edge term="C48" to="1"/>
            <scm:edge term="C64" to="1"/>
            <scm:edge term="C68" to="1"/>
            <scm:edge term="C108" to="1"/>
            <scm:edge term="C66" to="1"/>
            <scm:edge term="C60" to="1"/>
            <scm:edge term="C114" to="1"/>
            <scm:edge term="C121" to="1"/>
            <scm:edge term="C130" to="1"/>
            <scm:edge term="C99" to="1"/>
            <scm:edge term="C76" to="1"/>
            <scm:edge term="C74" to="1"/>
            <scm:edge term="C97" to="1"/>
            <scm:edge term="C58" to="1"/>
            <scm:edge term="C89" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C238"
                  name="base"
                  type="C8"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C242"
                  name="memberTypes"
                  type="C12"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C243"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C245"
                  name="targetNamespace"
                  type="C23"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C246"
                  name="variety"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C240"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C244"
                  name="primitiveType"
                  type="C9"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:complexType id="C50"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C247"/>
      <scm:attributeUse required="true" inheritable="false" ref="C248"/>
      <scm:attributeUse required="false" inheritable="false" ref="C249"/>
      <scm:attributeUse required="true" inheritable="false" ref="C250"/>
      <scm:attributeUse required="false" inheritable="false" ref="C251"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C251"
                  name="notQName"
                  type="C24"
                  global="false"
                  inheritable="false"
                  containingComplexType="C50"/>
   <scm:attribute id="C247"
                  name="constraint"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C50"/>
   <scm:attribute id="C250"
                  name="processContents"
                  type="C3"
                  global="false"
                  inheritable="false"
                  containingComplexType="C50"/>
   <scm:attribute id="C249"
                  name="namespaces"
                  type="C21"
                  global="false"
                  inheritable="false"
                  containingComplexType="C50"/>
   <scm:attribute id="C248"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C50"/>
   <scm:attribute id="C241"
                  name="itemType"
                  type="C8"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:complexType id="C59"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C252" default="false"/>
      <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C119"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C119" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C252"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C59"/>
   <scm:attribute id="C184"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C61"/>
   <scm:attribute id="C239"
                  name="final"
                  type="C0"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C233"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C174"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C113"/>
   <scm:attribute id="C146"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C90"/>
</scm:schema>
<?Σ e06d0fba?>
