<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:tei="http://www.tei-c.org/ns/1.0">

    <xsl:output method="text" omit-xml-declaration="yes" />
    <xsl:strip-space elements="*" />

    <xsl:template match="tei:head">
        <xsl:apply-templates />
        <xsl:if test="position() != last()"><xsl:text>&#10;&#10;</xsl:text></xsl:if>
    </xsl:template>

    <xsl:template match="tei:p | tei:lg | tei:table">
        <xsl:apply-templates />
        <xsl:if test="position() != last()"><xsl:text>&#10;</xsl:text></xsl:if>
    </xsl:template>

    <xsl:template match="tei:l">
        <xsl:apply-templates />
        <xsl:if test="position() != last()"><xsl:text>&#10;</xsl:text></xsl:if>
    </xsl:template>

    <xsl:template match="tei:lb">
        <xsl:text>&#10;</xsl:text>
    </xsl:template>

    <xsl:template match="tei:figure | tei:binaryObject">
        <xsl:text>
            [IMAGE].
            &#10;
        </xsl:text>
    </xsl:template>

    <xsl:template match="tei:label">
        <xsl:apply-templates />
        <xsl:text> </xsl:text>
    </xsl:template>

    <xsl:template match="tei:note">
        <xsl:text> [</xsl:text>
            <xsl:apply-templates />
        <xsl:text>]&#10;</xsl:text>
    </xsl:template>

</xsl:stylesheet>


