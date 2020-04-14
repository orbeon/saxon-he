<?xml version="1.0" encoding="UTF-8"?>
<scm:schema xmlns:scm="http://ns.saxonica.com/schema-component-model"
            generatedAt="2020-04-14T16:34:12.23+01:00"
            xsdVersion="1.0"
            dmk="TGljZW5zb3I9U2F4b25pY2EKTGljZW5zZWU9TWljaGFlbCBLYXkKQ29tcGFueT1TYXhvbmljYQpFbWFpbD1taWtlQHNheG9uaWNhLmNvbQpFZGl0aW9uPUVFClNBVD15ZXMKU0FRPXllcwpTQVY9eWVzCklzc3VlZD0yMDIwLTAzLTAxClNlcmllcz1WClNlcmlhbD1WMDA4NjgwClVzZXI9UDAwMDEKRXZhbHVhdGlvbj1ubwpFeHBpcmF0aW9uPTIwMjEtMDMtMzEKVXBncmFkZURheXM9MzY1Ck1haW50ZW5hbmNlRGF5cz0zNjUKClNpZ25hdHVyZT0zMDJDMDIxNDcwNjkxMzkxQjNCQ0MyQzlGRjUyMzQ2OERGODIwMzhBMjY0MDIxNDkwMjE0NDQ1MjU2MDYxMjAzRjhCOEI1OUE4NzVCMEUzOUNGN0U1Q0E1REMzOQ==">
   <scm:simpleType id="C0"
                   name="complexVarietyType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="mixed"/>
      <scm:enumeration value="simple"/>
      <scm:enumeration value="element-only"/>
      <scm:enumeration value="empty"/>
   </scm:simpleType>
   <scm:simpleType id="C1"
                   name="typeReferenceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="C2 #IDREF"/>
   <scm:simpleType id="C3"
                   name="notQNameListType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C4"/>
   <scm:simpleType id="C5"
                   name="xsdVersionType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="1.0"/>
      <scm:enumeration value="1.1"/>
   </scm:simpleType>
   <scm:simpleType id="C6"
                   name="openContentModeType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="suffix"/>
      <scm:enumeration value="interleave"/>
   </scm:simpleType>
   <scm:simpleType id="C4"
                   name="notQNameType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="C7 C8 #NCName"/>
   <scm:simpleType id="C2"
                   name="builtInTypeReferenceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value="#[a-zA-Z0-9]+\*?"/>
   </scm:simpleType>
   <scm:simpleType id="C9"
                   name="zero-length-string"
                   targetNamespace="http://saxon.sf.net/"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:length value="0"/>
   </scm:simpleType>
   <scm:complexType id="C10"
                    name="abstractParticleType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="true"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C11"/>
      <scm:attributeUse required="true" inheritable="false" ref="C12"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:simpleType id="C13"
                   name="xpathExpressionType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value=".+"/>
   </scm:simpleType>
   <scm:simpleType id="C14"
                   name="_langType"
                   targetNamespace="http://www.w3.org/XML/1998/namespace"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="#language C9"/>
   <scm:simpleType id="C15"
                   name="finalType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C16"/>
   <scm:simpleType id="C7"
                   name="pseudoQNameType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="##definedSibling"/>
      <scm:enumeration value="##defined"/>
   </scm:simpleType>
   <scm:simpleType id="C17"
                   name="blockType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C16"/>
   <scm:simpleType id="C18"
                   name="pseudoNamespaceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="##targetNamespace"/>
      <scm:enumeration value="##local"/>
      <scm:enumeration value="##any"/>
      <scm:enumeration value="##other"/>
      <scm:enumeration value="##defaultNamespace"/>
      <scm:enumeration value="##absent"/>
   </scm:simpleType>
   <scm:simpleType id="C19"
                   name="typeReferenceListType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C1"/>
   <scm:complexType id="C20"
                    name="xpathContainerType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C21"/>
      <scm:attributeUse required="true" inheritable="false" ref="C22"/>
      <scm:attributeUse required="false" inheritable="false" ref="C23"/>
      <scm:attributeUse required="false" inheritable="false" ref="C24"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:simpleType id="C25"
                   name="explicitTimezoneType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="optional"/>
      <scm:enumeration value="required"/>
      <scm:enumeration value="prohibited"/>
   </scm:simpleType>
   <scm:simpleType id="C8"
                   name="clarkNameType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#string"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value="\{[^{}]*\}\i\c*"/>
   </scm:simpleType>
   <scm:simpleType id="C26"
                   name="maxOccursType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="#nonNegativeInteger C27"/>
   <scm:simpleType id="C28"
                   name="namespaceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="union"
                   memberTypes="C18 C29"/>
   <scm:simpleType id="C30"
                   name="_spaceType"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="preserve"/>
      <scm:enumeration value="default"/>
   </scm:simpleType>
   <scm:simpleType id="C31"
                   name="namespaceListType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#anySimpleType"
                   variety="list"
                   itemType="C28"/>
   <scm:simpleType id="C32"
                   name="processContentsType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="lax"/>
      <scm:enumeration value="skip"/>
      <scm:enumeration value="strict"/>
   </scm:simpleType>
   <scm:complexType id="C33"
                    name="typedValueType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C34"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C35"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C35" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C35" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:simpleType id="C27"
                   name="unboundedType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="unbounded"/>
   </scm:simpleType>
   <scm:complexType id="C36"
                    name="identityConstraintType"
                    targetNamespace="http://ns.saxonica.com/schema-component-model"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C37"/>
      <scm:attributeUse required="true" inheritable="false" ref="C38"/>
      <scm:attributeUse required="false" inheritable="false" ref="C39"/>
      <scm:attributeUse required="false" inheritable="false" ref="C40"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C41"/>
            <scm:elementParticle minOccurs="1" maxOccurs="unbounded" ref="C42"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C41" to="1"/>
         </scm:state>
         <scm:state nr="1">
            <scm:edge term="C42" to="2"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C42" to="3"/>
         </scm:state>
         <scm:state nr="3" final="true">
            <scm:edge term="C42" to="3"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:simpleType id="C16"
                   name="derivationMethodType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="extension"/>
      <scm:enumeration value="substitution"/>
      <scm:enumeration value="union"/>
      <scm:enumeration value="list"/>
      <scm:enumeration value="restriction"/>
   </scm:simpleType>
   <scm:simpleType id="C43"
                   name="whitespaceType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#NCName"
                   variety="atomic"
                   primitiveType="#string">
      <scm:enumeration value="replace"/>
      <scm:enumeration value="preserve"/>
      <scm:enumeration value="collapse"/>
   </scm:simpleType>
   <scm:simpleType id="C29"
                   name="uriType"
                   targetNamespace="http://ns.saxonica.com/schema-component-model"
                   base="#token"
                   variety="atomic"
                   primitiveType="#string">
      <scm:pattern value="[^\s\r\n\t]*"/>
   </scm:simpleType>
   <scm:element id="C44"
                name="totalDigits"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C45"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C47"
                name="schema"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C48"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C49"
                name="identityConstraint"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C50"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C51"
                name="choice"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C52"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C53"/>
   </scm:element>
   <scm:element id="C54"
                name="element"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C55"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C56"
                name="state"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C57"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C58"
                name="notation"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C59"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C60"
                name="attributeUse"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C61"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C42"
                name="field"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C20"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C62"
                name="minExclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C63"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C64"
                name="attributeGroup"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C65"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C66"
                name="assertion"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C67"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C68"
                name="minScale"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C69"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C70"
                name="attributeWildcard"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C71"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C72"
                name="fixed"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C33"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C73"
                name="keyref"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C36"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C74"
                name="maxExclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C75"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C53"
                name="abstractModelGroup"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C52"
                global="true"
                nillable="false"
                abstract="true"/>
   <scm:element id="C76"
                name="length"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C77"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C41"
                name="selector"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C20"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C78"
                name="maxScale"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C79"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C80"
                name="elementParticle"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C81"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C82"/>
   </scm:element>
   <scm:element id="C83"
                name="complexType"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C84"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C85"
                name="default"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C33"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C86"
                name="minInclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C87"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C88"
                name="all"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C52"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C53"/>
   </scm:element>
   <scm:element id="C89"
                name="key"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C36"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C90"
                name="maxInclusive"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C91"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C92"
                name="pattern"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C93"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C82"
                name="abstractParticle"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C10"
                global="true"
                nillable="false"
                abstract="true"/>
   <scm:element id="C94"
                name="unique"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C36"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C95"
                name="wildcard"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C96"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C97"
                name="modelGroupDefinition"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C98"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C99"
                name="fractionDigits"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C100"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C101"
                name="explicitTimezone"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C102"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C103"
                name="edge"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C104"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C105"
                name="finiteStateMachine"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C106"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C107"
                name="elementWildcard"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C108"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C82"/>
   </scm:element>
   <scm:element id="C109"
                name="modelGroupParticle"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C110"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C82"/>
   </scm:element>
   <scm:element id="C111"
                name="substitutionGroupAffiliation"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C112"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C113"
                name="attribute"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C114"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C115"
                name="alternativeType"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C116"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C117"
                name="assert"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C118"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C119"
                name="enumeration"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C120"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C121"
                name="minLength"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C122"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C123"
                name="preprocess"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C124"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C125"
                name="whiteSpace"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C126"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C127"
                name="sequence"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C52"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C53"/>
   </scm:element>
   <scm:element id="C46"
                name="abstractFacet"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="#anyType"
                global="true"
                nillable="false"
                abstract="true"/>
   <scm:element id="C128"
                name="simpleType"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C129"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:element id="C130"
                name="maxLength"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C131"
                global="true"
                nillable="false"
                abstract="false">
      <scm:substitutionGroupAffiliation ref="C46"/>
   </scm:element>
   <scm:element id="C132"
                name="openContent"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C133"
                global="true"
                nillable="false"
                abstract="false"/>
   <scm:attribute id="C134"
                  name="space"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="C30"
                  global="true"
                  inheritable="false"/>
   <scm:attribute id="C135"
                  name="lang"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="C14"
                  global="true"
                  inheritable="false"/>
   <scm:attribute id="C24"
                  name="base"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="#anyURI"
                  global="true"
                  inheritable="false"/>
   <scm:attribute id="C136"
                  name="id"
                  targetNamespace="http://www.w3.org/XML/1998/namespace"
                  type="#ID"
                  global="true"
                  inheritable="false"/>
   <scm:attribute id="C11"
                  name="maxOccurs"
                  type="C26"
                  global="false"
                  inheritable="false"
                  containingComplexType="C10"/>
   <scm:attribute id="C12"
                  name="minOccurs"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C10"/>
   <scm:attribute id="C21"
                  name="xpath"
                  type="C13"
                  global="false"
                  inheritable="false"
                  containingComplexType="C20"/>
   <scm:attribute id="C22"
                  name="defaultNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C20"/>
   <scm:attribute id="C23"
                  name="type"
                  type="C2"
                  global="false"
                  inheritable="false"
                  containingComplexType="C20"/>
   <scm:attribute id="C34"
                  name="lexicalForm"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C33"/>
   <scm:element id="C35"
                name="item"
                targetNamespace="http://ns.saxonica.com/schema-component-model"
                type="C137"
                global="false"
                containingComplexType="C33"
                nillable="false"
                abstract="false"/>
   <scm:attribute id="C37"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C36"/>
   <scm:attribute id="C38"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C36"/>
   <scm:attribute id="C39"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C36"/>
   <scm:attribute id="C40"
                  name="key"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C36"/>
   <scm:complexType id="C45"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C138"/>
      <scm:attributeUse required="false" inheritable="false" ref="C139" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C48"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C140"/>
      <scm:attributeUse required="false" inheritable="false" ref="C141"/>
      <scm:attributeUse required="false" inheritable="false" ref="C142"/>
      <scm:modelGroupParticle minOccurs="0" maxOccurs="unbounded">
         <scm:choice>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C54"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C113"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C83"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C128"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C64"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C97"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C58"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C95"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C94"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C89"/>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C73"/>
         </scm:choice>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C128" to="1"/>
            <scm:edge term="C64" to="1"/>
            <scm:edge term="C54" to="1"/>
            <scm:edge term="C83" to="1"/>
            <scm:edge term="C97" to="1"/>
            <scm:edge term="C73" to="1"/>
            <scm:edge term="C89" to="1"/>
            <scm:edge term="C58" to="1"/>
            <scm:edge term="C113" to="1"/>
            <scm:edge term="C95" to="1"/>
            <scm:edge term="C94" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C128" to="1"/>
            <scm:edge term="C64" to="1"/>
            <scm:edge term="C54" to="1"/>
            <scm:edge term="C83" to="1"/>
            <scm:edge term="C97" to="1"/>
            <scm:edge term="C73" to="1"/>
            <scm:edge term="C89" to="1"/>
            <scm:edge term="C58" to="1"/>
            <scm:edge term="C113" to="1"/>
            <scm:edge term="C95" to="1"/>
            <scm:edge term="C94" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C50"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C143"/>
      <scm:attributeUse required="false" inheritable="false" ref="C24"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C52"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:modelGroupParticle minOccurs="0" maxOccurs="unbounded">
         <scm:sequence>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C82"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C109" to="1"/>
            <scm:edge term="C80" to="1"/>
            <scm:edge term="C107" to="1"/>
            <scm:edge term="C82" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C109" to="1"/>
            <scm:edge term="C80" to="1"/>
            <scm:edge term="C107" to="1"/>
            <scm:edge term="C82" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C55"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C144"/>
      <scm:attributeUse required="false" inheritable="false" ref="C145"/>
      <scm:attributeUse required="false" inheritable="false" ref="C146"/>
      <scm:attributeUse required="false" inheritable="false" ref="C147"/>
      <scm:attributeUse required="false" inheritable="false" ref="C148"/>
      <scm:attributeUse required="true" inheritable="false" ref="C149"/>
      <scm:attributeUse required="true" inheritable="false" ref="C150"/>
      <scm:attributeUse required="true" inheritable="false" ref="C151"/>
      <scm:attributeUse required="true" inheritable="false" ref="C152"/>
      <scm:attributeUse required="false" inheritable="false" ref="C153"/>
      <scm:attributeUse required="true" inheritable="false" ref="C154"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C111"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C115"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C49"/>
            <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
               <scm:choice>
                  <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C72"/>
                  <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C85"/>
               </scm:choice>
            </scm:modelGroupParticle>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C49" to="1"/>
            <scm:edge term="C111" to="2"/>
            <scm:edge term="C72" to="3"/>
            <scm:edge term="C85" to="3"/>
            <scm:edge term="C115" to="4"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C49" to="1"/>
            <scm:edge term="C72" to="3"/>
            <scm:edge term="C85" to="3"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C49" to="1"/>
            <scm:edge term="C111" to="2"/>
            <scm:edge term="C72" to="3"/>
            <scm:edge term="C85" to="3"/>
            <scm:edge term="C115" to="4"/>
         </scm:state>
         <scm:state nr="3" final="true"/>
         <scm:state nr="4" final="true">
            <scm:edge term="C49" to="1"/>
            <scm:edge term="C72" to="3"/>
            <scm:edge term="C85" to="3"/>
            <scm:edge term="C115" to="4"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C57"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C155"/>
      <scm:attributeUse required="false" inheritable="false" ref="C156"/>
      <scm:attributeUse required="false" inheritable="false" ref="C157"/>
      <scm:attributeUse required="false" inheritable="false" ref="C158"/>
      <scm:attributeUse required="true" inheritable="false" ref="C159"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C103"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C103" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C103" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C59"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C160"/>
      <scm:attributeUse required="false" inheritable="false" ref="C161"/>
      <scm:attributeUse required="false" inheritable="false" ref="C162"/>
      <scm:attributeUse required="false" inheritable="false" ref="C163"/>
      <scm:attributeUse required="false" inheritable="false" ref="C164"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C61"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C165"/>
      <scm:attributeUse required="true" inheritable="false" ref="C166"/>
      <scm:attributeUse required="true" inheritable="false" ref="C167"/>
      <scm:attributeUse required="true" inheritable="false" ref="C168"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:choice>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C72"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C85"/>
         </scm:choice>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C72" to="1"/>
            <scm:edge term="C85" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C63"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C169"/>
      <scm:attributeUse required="false" inheritable="false" ref="C170" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C65"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C171"/>
      <scm:attributeUse required="false" inheritable="false" ref="C172"/>
      <scm:attributeUse required="false" inheritable="false" ref="C173"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C60"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C70"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C70" to="1"/>
            <scm:edge term="C60" to="2"/>
         </scm:state>
         <scm:state nr="1" final="true"/>
         <scm:state nr="2" final="true">
            <scm:edge term="C70" to="1"/>
            <scm:edge term="C60" to="2"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C67"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C174"/>
      <scm:attributeUse required="true" inheritable="false" ref="C175"/>
      <scm:attributeUse required="false" inheritable="false" ref="C24"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C69"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C176"/>
      <scm:attributeUse required="false" inheritable="false" ref="C177" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C71"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C178"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C75"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C179"/>
      <scm:attributeUse required="false" inheritable="false" ref="C180" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C77"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C181"/>
      <scm:attributeUse required="false" inheritable="false" ref="C182" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C79"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C183"/>
      <scm:attributeUse required="false" inheritable="false" ref="C184" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C81"
                    base="C10"
                    derivationMethod="extension"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C185"/>
      <scm:attributeUse required="true" inheritable="false" ref="C11"/>
      <scm:attributeUse required="true" inheritable="false" ref="C12"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C84"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C186"/>
      <scm:attributeUse required="true" inheritable="false" ref="C187"/>
      <scm:attributeUse required="true" inheritable="false" ref="C188"/>
      <scm:attributeUse required="false" inheritable="false" ref="C189"/>
      <scm:attributeUse required="false" inheritable="false" ref="C190"/>
      <scm:attributeUse required="true" inheritable="false" ref="C191"/>
      <scm:attributeUse required="false" inheritable="false" ref="C192"/>
      <scm:attributeUse required="false" inheritable="false" ref="C193"/>
      <scm:attributeUse required="false" inheritable="false" ref="C194"/>
      <scm:attributeUse required="true" inheritable="false" ref="C195"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:sequence>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C132"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C60"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C70"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C82"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C105"/>
            <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C66"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C80" to="2"/>
            <scm:edge term="C70" to="4"/>
            <scm:edge term="C132" to="5"/>
            <scm:edge term="C109" to="2"/>
            <scm:edge term="C82" to="2"/>
            <scm:edge term="C105" to="1"/>
            <scm:edge term="C60" to="3"/>
            <scm:edge term="C107" to="2"/>
            <scm:edge term="C66" to="6"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C66" to="6"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C105" to="1"/>
            <scm:edge term="C66" to="6"/>
         </scm:state>
         <scm:state nr="3" final="true">
            <scm:edge term="C105" to="1"/>
            <scm:edge term="C80" to="2"/>
            <scm:edge term="C60" to="3"/>
            <scm:edge term="C109" to="2"/>
            <scm:edge term="C70" to="4"/>
            <scm:edge term="C82" to="2"/>
            <scm:edge term="C66" to="6"/>
            <scm:edge term="C107" to="2"/>
         </scm:state>
         <scm:state nr="4" final="true">
            <scm:edge term="C105" to="1"/>
            <scm:edge term="C80" to="2"/>
            <scm:edge term="C109" to="2"/>
            <scm:edge term="C82" to="2"/>
            <scm:edge term="C66" to="6"/>
            <scm:edge term="C107" to="2"/>
         </scm:state>
         <scm:state nr="5" final="true">
            <scm:edge term="C105" to="1"/>
            <scm:edge term="C80" to="2"/>
            <scm:edge term="C60" to="3"/>
            <scm:edge term="C109" to="2"/>
            <scm:edge term="C70" to="4"/>
            <scm:edge term="C82" to="2"/>
            <scm:edge term="C66" to="6"/>
            <scm:edge term="C107" to="2"/>
         </scm:state>
         <scm:state nr="6" final="true">
            <scm:edge term="C66" to="6"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C87"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C196"/>
      <scm:attributeUse required="false" inheritable="false" ref="C197" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C91"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C198"/>
      <scm:attributeUse required="false" inheritable="false" ref="C199" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C93"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C200"/>
      <scm:attributeUse required="false" inheritable="false" ref="C201" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C96"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C202"/>
      <scm:attributeUse required="true" inheritable="false" ref="C203"/>
      <scm:attributeUse required="false" inheritable="false" ref="C204"/>
      <scm:attributeUse required="true" inheritable="false" ref="C205"/>
      <scm:attributeUse required="false" inheritable="false" ref="C206"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C98"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C207"/>
      <scm:attributeUse required="false" inheritable="false" ref="C208"/>
      <scm:attributeUse required="false" inheritable="false" ref="C209"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C82"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C80" to="1"/>
            <scm:edge term="C109" to="1"/>
            <scm:edge term="C107" to="1"/>
            <scm:edge term="C82" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C80" to="1"/>
            <scm:edge term="C109" to="1"/>
            <scm:edge term="C107" to="1"/>
            <scm:edge term="C82" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C100"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C210"/>
      <scm:attributeUse required="false" inheritable="false" ref="C211" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C102"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C212"/>
      <scm:attributeUse required="false" inheritable="false" ref="C213" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C104"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C214"/>
      <scm:attributeUse required="true" inheritable="false" ref="C215"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C106"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C216"/>
      <scm:elementParticle minOccurs="1" maxOccurs="unbounded" ref="C56"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C56" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C56" to="2"/>
         </scm:state>
         <scm:state nr="2" final="true">
            <scm:edge term="C56" to="2"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C108"
                    base="C10"
                    derivationMethod="extension"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C217"/>
      <scm:attributeUse required="true" inheritable="false" ref="C11"/>
      <scm:attributeUse required="true" inheritable="false" ref="C12"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C110"
                    base="C10"
                    derivationMethod="extension"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C218"/>
      <scm:attributeUse required="true" inheritable="false" ref="C11"/>
      <scm:attributeUse required="true" inheritable="false" ref="C12"/>
      <scm:elementParticle minOccurs="0" maxOccurs="unbounded" ref="C53"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C51" to="1"/>
            <scm:edge term="C88" to="1"/>
            <scm:edge term="C127" to="1"/>
            <scm:edge term="C53" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C51" to="1"/>
            <scm:edge term="C88" to="1"/>
            <scm:edge term="C127" to="1"/>
            <scm:edge term="C53" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C112"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C219"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C114"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C220"/>
      <scm:attributeUse required="false" inheritable="false" ref="C221"/>
      <scm:attributeUse required="true" inheritable="false" ref="C222"/>
      <scm:attributeUse required="true" inheritable="false" ref="C223"/>
      <scm:attributeUse required="true" inheritable="false" ref="C224"/>
      <scm:attributeUse required="false" inheritable="false" ref="C225"/>
      <scm:attributeUse required="true" inheritable="false" ref="C226"/>
      <scm:attributeUse required="true" inheritable="false" ref="C227"/>
      <scm:modelGroupParticle minOccurs="1" maxOccurs="1">
         <scm:choice>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C72"/>
            <scm:elementParticle minOccurs="0" maxOccurs="1" ref="C85"/>
         </scm:choice>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C72" to="1"/>
            <scm:edge term="C85" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C116"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C228"/>
      <scm:attributeUse required="true" inheritable="false" ref="C229"/>
      <scm:attributeUse required="true" inheritable="false" ref="C230"/>
      <scm:attributeUse required="false" inheritable="false" ref="C24"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C118"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C231" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C66"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C66" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C120"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C232"/>
      <scm:attributeUse required="false" inheritable="false" ref="C233" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C122"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C234"/>
      <scm:attributeUse required="false" inheritable="false" ref="C235" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C124"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="false" inheritable="false" ref="C236" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:elementParticle minOccurs="1" maxOccurs="2" ref="C66"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0">
            <scm:edge term="C66" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true" minOccurs="1" maxOccurs="2">
            <scm:edge term="C66" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C126"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C237"/>
      <scm:attributeUse required="false" inheritable="false" ref="C238" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C129"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="element-only">
      <scm:attributeUse required="true" inheritable="false" ref="C239"/>
      <scm:attributeUse required="false" inheritable="false" ref="C240"/>
      <scm:attributeUse required="true" inheritable="false" ref="C241"/>
      <scm:attributeUse required="false" inheritable="false" ref="C242"/>
      <scm:attributeUse required="false" inheritable="false" ref="C243"/>
      <scm:attributeUse required="false" inheritable="false" ref="C244"/>
      <scm:attributeUse required="false" inheritable="false" ref="C245"/>
      <scm:attributeUse required="false" inheritable="false" ref="C246"/>
      <scm:attributeUse required="true" inheritable="false" ref="C247"/>
      <scm:modelGroupParticle minOccurs="0" maxOccurs="unbounded">
         <scm:sequence>
            <scm:elementParticle minOccurs="1" maxOccurs="1" ref="C46"/>
         </scm:sequence>
      </scm:modelGroupParticle>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true">
            <scm:edge term="C121" to="1"/>
            <scm:edge term="C117" to="1"/>
            <scm:edge term="C90" to="1"/>
            <scm:edge term="C130" to="1"/>
            <scm:edge term="C44" to="1"/>
            <scm:edge term="C62" to="1"/>
            <scm:edge term="C46" to="1"/>
            <scm:edge term="C68" to="1"/>
            <scm:edge term="C101" to="1"/>
            <scm:edge term="C99" to="1"/>
            <scm:edge term="C74" to="1"/>
            <scm:edge term="C78" to="1"/>
            <scm:edge term="C92" to="1"/>
            <scm:edge term="C119" to="1"/>
            <scm:edge term="C76" to="1"/>
            <scm:edge term="C123" to="1"/>
            <scm:edge term="C86" to="1"/>
            <scm:edge term="C125" to="1"/>
         </scm:state>
         <scm:state nr="1" final="true">
            <scm:edge term="C121" to="1"/>
            <scm:edge term="C117" to="1"/>
            <scm:edge term="C90" to="1"/>
            <scm:edge term="C130" to="1"/>
            <scm:edge term="C44" to="1"/>
            <scm:edge term="C62" to="1"/>
            <scm:edge term="C46" to="1"/>
            <scm:edge term="C68" to="1"/>
            <scm:edge term="C101" to="1"/>
            <scm:edge term="C99" to="1"/>
            <scm:edge term="C74" to="1"/>
            <scm:edge term="C78" to="1"/>
            <scm:edge term="C92" to="1"/>
            <scm:edge term="C119" to="1"/>
            <scm:edge term="C76" to="1"/>
            <scm:edge term="C123" to="1"/>
            <scm:edge term="C86" to="1"/>
            <scm:edge term="C125" to="1"/>
         </scm:state>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C131"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C248"/>
      <scm:attributeUse required="false" inheritable="false" ref="C249" default="false">
         <scm:default lexicalForm="false">
            <scm:item type="#boolean" value="false"/>
         </scm:default>
      </scm:attributeUse>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C133"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="false" inheritable="false" ref="C250"/>
      <scm:attributeUse required="false" inheritable="false" ref="C251"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:complexType id="C137"
                    base="#anyType"
                    derivationMethod="restriction"
                    abstract="false"
                    variety="empty">
      <scm:attributeUse required="true" inheritable="false" ref="C252"/>
      <scm:attributeUse required="true" inheritable="false" ref="C253"/>
      <scm:attributeUse required="false" inheritable="false" ref="C254"/>
      <scm:finiteStateMachine initialState="0">
         <scm:state nr="0" final="true"/>
      </scm:finiteStateMachine>
   </scm:complexType>
   <scm:attribute id="C138"
                  name="value"
                  type="#positiveInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C139"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C45"/>
   <scm:attribute id="C140"
                  name="generatedAt"
                  type="#dateTime"
                  global="false"
                  inheritable="false"
                  containingComplexType="C48"/>
   <scm:attribute id="C141"
                  name="xsdVersion"
                  type="C5"
                  global="false"
                  inheritable="false"
                  containingComplexType="C48"/>
   <scm:attribute id="C142"
                  name="dmk"
                  type="#base64Binary"
                  global="false"
                  inheritable="false"
                  containingComplexType="C48"/>
   <scm:attribute id="C143"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C50"/>
   <scm:attribute id="C144"
                  name="abstract"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C145"
                  name="block"
                  type="C17"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C146"
                  name="containingComplexType"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C147"
                  name="default"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C148"
                  name="final"
                  type="C15"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C149"
                  name="global"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C150"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C151"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C152"
                  name="nillable"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C153"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C154"
                  name="type"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C55"/>
   <scm:attribute id="C155"
                  name="afterMax"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C156"
                  name="final"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C157"
                  name="maxOccurs"
                  type="C26"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C158"
                  name="minOccurs"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C159"
                  name="nr"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C57"/>
   <scm:attribute id="C160"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C59"/>
   <scm:attribute id="C161"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C59"/>
   <scm:attribute id="C162"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C59"/>
   <scm:attribute id="C163"
                  name="systemId"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C59"/>
   <scm:attribute id="C164"
                  name="publicId"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C59"/>
   <scm:attribute id="C165"
                  name="default"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C61"/>
   <scm:attribute id="C166"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C61"/>
   <scm:attribute id="C167"
                  name="required"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C61"/>
   <scm:attribute id="C168"
                  name="inheritable"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C61"/>
   <scm:attribute id="C169"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C63"/>
   <scm:attribute id="C170"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C63"/>
   <scm:attribute id="C171"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C65"/>
   <scm:attribute id="C172"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C65"/>
   <scm:attribute id="C173"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C65"/>
   <scm:attribute id="C174"
                  name="test"
                  type="C13"
                  global="false"
                  inheritable="false"
                  containingComplexType="C67"/>
   <scm:attribute id="C175"
                  name="defaultNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C67"/>
   <scm:attribute id="C176"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C69"/>
   <scm:attribute id="C177"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C69"/>
   <scm:attribute id="C178"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C71"/>
   <scm:attribute id="C179"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C75"/>
   <scm:attribute id="C180"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C75"/>
   <scm:attribute id="C181"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C77"/>
   <scm:attribute id="C182"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C77"/>
   <scm:attribute id="C183"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C79"/>
   <scm:attribute id="C184"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C79"/>
   <scm:attribute id="C185"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C81"/>
   <scm:attribute id="C186"
                  name="abstract"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C187"
                  name="base"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C188"
                  name="derivationMethod"
                  type="C16"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C189"
                  name="block"
                  type="C17"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C190"
                  name="final"
                  type="C15"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C191"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C192"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C193"
                  name="simpleType"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C194"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C195"
                  name="variety"
                  type="C0"
                  global="false"
                  inheritable="false"
                  containingComplexType="C84"/>
   <scm:attribute id="C196"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C87"/>
   <scm:attribute id="C197"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C87"/>
   <scm:attribute id="C198"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C91"/>
   <scm:attribute id="C199"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C91"/>
   <scm:attribute id="C200"
                  name="value"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C93"/>
   <scm:attribute id="C201"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C93"/>
   <scm:attribute id="C202"
                  name="constraint"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C96"/>
   <scm:attribute id="C203"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C96"/>
   <scm:attribute id="C204"
                  name="namespaces"
                  type="C31"
                  global="false"
                  inheritable="false"
                  containingComplexType="C96"/>
   <scm:attribute id="C205"
                  name="processContents"
                  type="C32"
                  global="false"
                  inheritable="false"
                  containingComplexType="C96"/>
   <scm:attribute id="C206"
                  name="notQName"
                  type="C3"
                  global="false"
                  inheritable="false"
                  containingComplexType="C96"/>
   <scm:attribute id="C207"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C98"/>
   <scm:attribute id="C208"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C98"/>
   <scm:attribute id="C209"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C98"/>
   <scm:attribute id="C210"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C100"/>
   <scm:attribute id="C211"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C100"/>
   <scm:attribute id="C212"
                  name="value"
                  type="C25"
                  global="false"
                  inheritable="false"
                  containingComplexType="C102"/>
   <scm:attribute id="C213"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C102"/>
   <scm:attribute id="C214"
                  name="term"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C104"/>
   <scm:attribute id="C215"
                  name="to"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C104"/>
   <scm:attribute id="C216"
                  name="initialState"
                  type="#integer"
                  global="false"
                  inheritable="false"
                  containingComplexType="C106"/>
   <scm:attribute id="C217"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C108"/>
   <scm:attribute id="C218"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C110"/>
   <scm:attribute id="C219"
                  name="ref"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C112"/>
   <scm:attribute id="C220"
                  name="containingComplexType"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C221"
                  name="default"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C222"
                  name="global"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C223"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C224"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C225"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C226"
                  name="type"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C227"
                  name="inheritable"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C114"/>
   <scm:attribute id="C228"
                  name="type"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C116"/>
   <scm:attribute id="C229"
                  name="test"
                  type="C13"
                  global="false"
                  inheritable="false"
                  containingComplexType="C116"/>
   <scm:attribute id="C230"
                  name="defaultNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C116"/>
   <scm:attribute id="C231"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C118"/>
   <scm:attribute id="C232"
                  name="value"
                  type="#anySimpleType"
                  global="false"
                  inheritable="false"
                  containingComplexType="C120"/>
   <scm:attribute id="C233"
                  name="namespaceSensitive"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C120"/>
   <scm:attribute id="C234"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C122"/>
   <scm:attribute id="C235"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C122"/>
   <scm:attribute id="C236"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C124"/>
   <scm:attribute id="C237"
                  name="value"
                  type="C43"
                  global="false"
                  inheritable="false"
                  containingComplexType="C126"/>
   <scm:attribute id="C238"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C126"/>
   <scm:attribute id="C239"
                  name="base"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C240"
                  name="final"
                  type="C15"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C241"
                  name="id"
                  type="#ID"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C242"
                  name="itemType"
                  type="C1"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C243"
                  name="memberTypes"
                  type="C19"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C244"
                  name="name"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C245"
                  name="primitiveType"
                  type="C2"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C246"
                  name="targetNamespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C247"
                  name="variety"
                  type="#NCName"
                  global="false"
                  inheritable="false"
                  containingComplexType="C129"/>
   <scm:attribute id="C248"
                  name="value"
                  type="#nonNegativeInteger"
                  global="false"
                  inheritable="false"
                  containingComplexType="C131"/>
   <scm:attribute id="C249"
                  name="fixed"
                  type="#boolean"
                  global="false"
                  inheritable="false"
                  containingComplexType="C131"/>
   <scm:attribute id="C250"
                  name="mode"
                  type="C6"
                  global="false"
                  inheritable="false"
                  containingComplexType="C133"/>
   <scm:attribute id="C251"
                  name="wildcard"
                  type="#IDREF"
                  global="false"
                  inheritable="false"
                  containingComplexType="C133"/>
   <scm:attribute id="C252"
                  name="type"
                  type="C2"
                  global="false"
                  inheritable="false"
                  containingComplexType="C137"/>
   <scm:attribute id="C253"
                  name="value"
                  type="#string"
                  global="false"
                  inheritable="false"
                  containingComplexType="C137"/>
   <scm:attribute id="C254"
                  name="namespace"
                  type="C29"
                  global="false"
                  inheritable="false"
                  containingComplexType="C137"/>
</scm:schema>
<?Σ a63eb46d?>
