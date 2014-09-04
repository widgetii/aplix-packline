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

	<!-- ============== -->
	<!-- template: CDEK -->
	<!-- ============== -->
	<xsl:template match="Report[@FileVersion = '172']">
		<!-- Determine appropriate page size -->
		<xsl:variable name="pageSize">
			<xsl:choose>
				<xsl:when test="count(//Dataset[@Name = 'EnclosuresDataSet']/Rows/Row) &lt;= 3">
					<xsl:value-of select="'A5'" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="'A4'" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:copy>
			<!-- Copy all attributes from current node -->
			<xsl:apply-templates select="@*" />

			<!-- Enumerate all child node -->
			<xsl:for-each select="node()">
				<xsl:choose>
					<!-- Copy only specific page -->
					<xsl:when test="name() = 'Pages'">
						<xsl:copy>
							<xsl:for-each select="Page">
								<xsl:if test="$pageSize = concat('A', @Size)">
									<xsl:copy>
										<xsl:apply-templates select="@*|node()" />
									</xsl:copy>
								</xsl:if>
							</xsl:for-each>
						</xsl:copy>
					</xsl:when>
					<!-- Copy all other nodes without changes -->
					<xsl:otherwise>
						<xsl:copy>
							<xsl:apply-templates select="@*|node()" />
						</xsl:copy>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
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
					<xsl:when test="$post-type = 'DHL'">
						<xsl:call-template name="zebra-label">
							<xsl:with-param name="post-type-prefix" select="'DHL_'" />
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

	<!-- =============================== -->
	<!-- template: ezpl-barcode-report -->
	<!-- =============================== -->
	<xsl:template match="Report[@FileVersion = '1100']">
		<xsl:variable name="newline">
			<xsl:text>&#13;&#10;</xsl:text>
		</xsl:variable>

		<xsl:variable name="bar-code-type">
			<xsl:value-of select="//BarCodeView/@BarCodeType" />
		</xsl:variable>

		<xsl:variable name="print-mode">
			<xsl:value-of select="//Variable[@Name = 'PrintMode']" />
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$print-mode = 'EZPL'">
				<xsl:call-template name="ezpl-barcodes">
					<xsl:with-param name="newline" select="$newline" />
					<xsl:with-param name="bar-code-type" select="$bar-code-type" />
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="$print-mode = 'ZPL2'">
				<xsl:call-template name="zpl2-barcodes">
					<xsl:with-param name="newline" select="$newline" />
					<xsl:with-param name="bar-code-type" select="$bar-code-type" />
				</xsl:call-template>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<!-- =============================== -->
	<!-- template: ezpl-barcodes -->
	<!-- =============================== -->
	<xsl:template name="ezpl-barcodes">
		<xsl:param name="newline" />
		<xsl:param name="bar-code-type" />

		<!-- Change resolution according to used printer -->
		<!-- Defined values: 203, 300 -->
		<xsl:variable name="resolution">
			<xsl:text>300</xsl:text>
		</xsl:variable>

		<xsl:result-document method="text" indent="no" encoding="ASCII">
			<!-- Print bar codes -->
			<xsl:for-each select="//Column">
				<!-- Setup and Control Commands -->
				<xsl:value-of select="concat('^Q20,3', $newline)" />
				<xsl:value-of select="concat('^W30', $newline)" />
				<xsl:value-of select="concat('^H10', $newline)" />
				<xsl:value-of select="concat('^P1', $newline)" />
				<xsl:value-of select="concat('^S4', $newline)" />
				<xsl:value-of select="concat('^AT', $newline)" />
				<xsl:value-of select="concat('^C1', $newline)" />
				<xsl:value-of select="concat('^R0', $newline)" />
				<xsl:value-of select="concat('~Q+0', $newline)" />
				<xsl:value-of select="concat('^O0', $newline)" />
				<xsl:value-of select="concat('^D0', $newline)" />
				<xsl:value-of select="concat('^E0', $newline)" />
				<xsl:value-of select="concat('~R200', $newline)" />
				<xsl:value-of select="concat('^L', $newline)" />
				<xsl:value-of select="concat('Dy2-me-dd', $newline)" />
				<xsl:value-of select="concat('Th:m:s', $newline)" />
				<xsl:value-of select="$newline" />
				<xsl:choose>
					<xsl:when test="$bar-code-type = 'code39'">
						<xsl:choose>
							<xsl:when test="$resolution = '203'">
								<xsl:text>BA3,2,15,2,3,55,0,1,</xsl:text>
								<xsl:value-of select="concat(current(), $newline)" />
							</xsl:when>
							<xsl:when test="$resolution = '300'">
								<xsl:text>BA3,4,20,2,5,82,0,3,</xsl:text>
								<xsl:value-of select="concat(current(), $newline)" />
							</xsl:when>
						</xsl:choose>
					</xsl:when>
					<xsl:when test="$bar-code-type = 'ean-8'">
						<xsl:choose>
							<xsl:when test="$resolution = '203'">
								<xsl:text>BB,00,14,3,07,100,0,1,</xsl:text>
								<xsl:value-of select="concat(current(), $newline)" />
							</xsl:when>
							<xsl:when test="$resolution = '300'">
								<xsl:text>BB,24,22,4,10,160,0,3,</xsl:text>
								<xsl:value-of select="concat(current(), $newline)" />
							</xsl:when>
						</xsl:choose>
					</xsl:when>
				</xsl:choose>
				<xsl:value-of select="$newline" />
				<xsl:value-of select="concat('E', $newline)" />
			</xsl:for-each>
		</xsl:result-document>
	</xsl:template>

	<!-- =============================== -->
	<!-- template: zpl2-barcodes -->
	<!-- =============================== -->
	<xsl:template name="zpl2-barcodes">
		<xsl:param name="newline" />
		<xsl:param name="bar-code-type" />

		<xsl:result-document method="text" indent="no" encoding="ASCII">
			<!-- Setup and Control Commands -->
			<xsl:value-of select="concat('^CT~~CD,~CC^~CT~', $newline)" />
			<xsl:value-of select="concat('^XA~TA000~JSN^LT0^MNW^MTT^PON^PMN^LH0,0^JMA^PR3,3~SD10^JUS^LRN^CI0^XZ', $newline)" />
			<!-- Print bar codes -->
			<xsl:for-each select="//Column">
				<xsl:value-of select="concat('^XA', $newline)" />
				<xsl:value-of select="concat('^MMT', $newline)" />
				<xsl:value-of select="concat('^PW354', $newline)" />
				<xsl:value-of select="concat('^LL0295', $newline)" />
				<xsl:value-of select="concat('^LS0', $newline)" />
				<xsl:choose>
					<xsl:when test="$bar-code-type = 'code39'">
						<xsl:value-of select="concat('^FT47,236^A0N,50,69^FH\^FD', current())" />
						<xsl:value-of select="concat('^FS', $newline)" />
						<xsl:value-of select="concat('^BY2,3,144^FT16,171^B3N,N,,N,N', $newline)" />
						<xsl:value-of select="concat('^FD', current())" />
						<xsl:value-of select="concat('^FS', $newline)" />
					</xsl:when>
					<xsl:when test="$bar-code-type = 'ean-8'">
						<xsl:value-of select="concat('^BY4,2,235^FT44,252^B8N,,Y,N', $newline)" />
						<xsl:value-of select="concat('^FD', current())" />
						<xsl:value-of select="concat('^FS', $newline)" />
					</xsl:when>
				</xsl:choose>
				<xsl:value-of select="concat('^PQ1,0,1,Y^XZ', $newline)" />
			</xsl:for-each>
		</xsl:result-document>
	</xsl:template>

</xsl:stylesheet>
