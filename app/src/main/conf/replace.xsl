<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" omit-xml-declaration="no" indent="yes" standalone="yes" />

	<!-- ================= -->
	<!-- Identity Template -->
	<!-- ================= -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

	<!-- =============================== -->
	<!-- template: zebra-prepared-report -->
	<!-- =============================== -->
	<xsl:template match="Report[@FileVersion = '105']">
		<xsl:result-document standalone="no" doctype-system="label.dtd">
			<labels>
				<xsl:variable name="post-type">
					<xsl:value-of select="//Variable[@Name = 'POSTTYPE']" />
				</xsl:variable>

				<xsl:choose>
					<xsl:when test="$post-type = 'FIRSTCLASS'">
						<xsl:call-template name="zebra-label">
							<xsl:with-param name="post-type-prefix" select="'f7a_'" />
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="$post-type = 'PARCEL'">
						<xsl:call-template name="zebra-label">
							<xsl:with-param name="post-type-prefix" select="'f7p_'" />
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="$post-type = 'BOOKPOST'">
						<xsl:call-template name="zebra-label">
							<xsl:with-param name="post-type-prefix" select="'f7b_'" />
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="$post-type = 'LETTER'">
						<xsl:call-template name="zebra-label">
							<xsl:with-param name="post-type-prefix" select="'f7l_'" />
						</xsl:call-template>
					</xsl:when>
				</xsl:choose>
			</labels>
		</xsl:result-document>
	</xsl:template>

	<!-- ===================== -->
	<!-- template: zebra-label -->
	<!-- ===================== -->
	<xsl:template name="zebra-label">
		<xsl:param name="post-type-prefix" />

		<xsl:attribute name="_FORMAT"><xsl:value-of select="//Variable[@Name=concat($post-type-prefix, '_FORMAT')]/@Content" /></xsl:attribute>
		<xsl:attribute name="_QUANTITY"><xsl:value-of select="count(/Report/Pages/Page)" /></xsl:attribute>
		<xsl:attribute name="_PRINTERNAME"><xsl:value-of select="attribute::PrinterName" /></xsl:attribute>
		<xsl:attribute name="_JOBNAME"><xsl:value-of select="//Variable[@Name='_JOBNAME']/@Content" /></xsl:attribute>

		<label>
			<xsl:for-each select="//Column[@Name != 'POSTTYPE']">
				<xsl:if test="starts-with(attribute::Name, $post-type-prefix)">
					<variable>
						<xsl:attribute name="name"><xsl:value-of select="substring-after(attribute::Name, $post-type-prefix)" /></xsl:attribute>
						<xsl:value-of select="current()" />
					</variable>
				</xsl:if>
			</xsl:for-each>
		</label>
	</xsl:template>

</xsl:stylesheet>
