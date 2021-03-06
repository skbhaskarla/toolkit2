<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<!DOCTYPE schema [
<!-- Replace baseURI below with a reference to the published Implementation Guide HTML. -->
<!ENTITY baseURI "">
<!ENTITY ent-PRPA_IN201309UV02 SYSTEM 'PRPA_IN201309UV02.ent'>
]>
<schema xmlns="http://www.ascc.net/xml/schematron" xmlns:msg="urn:hl7-org:v3">
    <!-- 
        To use iso schematron instead of schematron 1.5, 
        change the xmlns attribute from
        "http://www.ascc.net/xml/schematron" 
        to 
        "http://purl.oclc.org/dsdl/schematron"
    -->
    
    <title>PRPA_IN201309UV02</title>
    <ns prefix="msg" uri="urn:hl7-org:v3"/>
    
    <phase id='errors'>
        <active pattern='PRPA_IN201309UV02-errors'/>
    </phase>
    
    <!--
        <phase id='warning'>
        <active pattern=' '/>
        </phase>
        
        <phase id='manual'>
        <active pattern=''/>
        </phase>
    -->
    
        <phase id='note'>
        <active pattern='PRPA_IN201309UV02-note'/>
        </phase>
        
    
    
    
    &ent-PRPA_IN201309UV02;
    
</schema>
