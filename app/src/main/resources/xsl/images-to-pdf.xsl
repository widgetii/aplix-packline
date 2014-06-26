<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" omit-xml-declaration="no" indent="yes" />

	<xsl:variable name="pageWidth">210mm</xsl:variable>
	<xsl:variable name="pageHeight">297mm</xsl:variable>

	<!-- ============ -->
	<!-- root element -->
	<!-- ============ -->
	<xsl:template match="files">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<xsl:call-template name="page-master" />
			</fo:layout-master-set>

			<xsl:choose>
				<xsl:when test="count(file) = 0">
					<xsl:call-template name="page-sequence" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:for-each select="file">
						<xsl:call-template name="page-sequence">
							<xsl:with-param name="fileName" select="current()" />
						</xsl:call-template>
					</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
		</fo:root>
	</xsl:template>

	<!-- ===================== -->
	<!-- template: page-master -->
	<!-- ===================== -->
	<xsl:template name="page-master">
		<fo:simple-page-master master-name="page">
			<!-- Width/Height -->
			<xsl:attribute name="page-width"><xsl:value-of select="$pageWidth" /></xsl:attribute>
			<xsl:attribute name="page-height"><xsl:value-of select="$pageHeight" /></xsl:attribute>

			<!-- Margins -->
			<xsl:attribute name="margin-top">0mm</xsl:attribute>
			<xsl:attribute name="margin-left">0mm</xsl:attribute>
			<xsl:attribute name="margin-right">0mm</xsl:attribute>
			<xsl:attribute name="margin-bottom">0mm</xsl:attribute>

			<!-- Page body -->
			<fo:region-body />
		</fo:simple-page-master>
	</xsl:template>

	<!-- ======================= -->
	<!-- template: page-sequence -->
	<!-- ======================= -->
	<xsl:template name="page-sequence">
		<xsl:param name="fileName" required="no" />

		<fo:page-sequence master-reference="page">
			<fo:flow flow-name="xsl-region-body">

				<fo:block-container>
					<xsl:attribute name="absolute-position">absolute</xsl:attribute>
					<xsl:attribute name="left">0</xsl:attribute>
					<xsl:attribute name="top">0</xsl:attribute>
					<xsl:attribute name="width">100%</xsl:attribute>
					<xsl:attribute name="height">100%</xsl:attribute>
					<xsl:attribute name="background-color">transparent</xsl:attribute>
					<xsl:attribute name="border-color">#000000</xsl:attribute>
					<xsl:attribute name="border-width">1.0pt</xsl:attribute>
					<xsl:attribute name="border-style">none</xsl:attribute>
					<xsl:attribute name="display-align">center</xsl:attribute>

					<fo:block>
						<xsl:attribute name="font-size">0</xsl:attribute>
						<xsl:attribute name="text-align">center</xsl:attribute>

						<xsl:if test="$fileName">
							<fo:external-graphic>
								<xsl:attribute name="scaling">uniform</xsl:attribute>
								<xsl:attribute name="content-width"><xsl:value-of select="$pageWidth" /></xsl:attribute>
								<xsl:attribute name="content-height"><xsl:value-of select="$pageHeight" /></xsl:attribute>
								<xsl:attribute name="src">
								<xsl:text>url('</xsl:text>
								<xsl:value-of select="$fileName" />
								<xsl:text>')</xsl:text>
							</xsl:attribute>
							</fo:external-graphic>
						</xsl:if>
					</fo:block>
				</fo:block-container>
			</fo:flow>
		</fo:page-sequence>
	</xsl:template>

</xsl:stylesheet>
