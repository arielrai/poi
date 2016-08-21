/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package org.apache.poi.xslf.usermodel;

import static org.apache.poi.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.util.Beta;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColorMapping;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextListStyle;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPlaceholder;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideMaster;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlideMasterTextStyles;
import org.openxmlformats.schemas.presentationml.x2006.main.SldMasterDocument;

/**
* Slide master object associated with this layout.
* <p>
*  Within a slide master slide are contained all elements
* that describe the objects and their corresponding formatting
* for within a presentation slide.
* </p>
* <p>
* Within a slide master slide are two main elements.
* The cSld element specifies the common slide elements such as shapes and
* their attached text bodies. Then the txStyles element specifies the
* formatting for the text within each of these shapes. The other properties
* within a slide master slide specify other properties for within a presentation slide
* such as color information, headers and footers, as well as timing and
* transition information for all corresponding presentation slides.
* </p>
 *
 * @author Yegor Kozlov
*/
@Beta
 public class XSLFSlideMaster extends XSLFSheet
 implements MasterSheet<XSLFShape,XSLFTextParagraph> {
	private CTSlideMaster _slide;
    private Map<String, XSLFSlideLayout> _layouts;
    private XSLFTheme _theme;

    XSLFSlideMaster() {
        super();
        _slide = CTSlideMaster.Factory.newInstance();
    }

    /**
     * @since POI 3.14-Beta1
     */
    protected XSLFSlideMaster(PackagePart part) throws IOException, XmlException {
        super(part);
        SldMasterDocument doc =
            SldMasterDocument.Factory.parse(getPackagePart().getInputStream(), DEFAULT_XML_OPTIONS);
        _slide = doc.getSldMaster();
        setCommonSlideData(_slide.getCSld());
    }

    /**
     * @deprecated in POI 3.14, scheduled for removal in POI 3.16
     */
    @Deprecated
    protected XSLFSlideMaster(PackagePart part, PackageRelationship rel) throws IOException, XmlException {
        this(part);
    }
    
    @Override
	public CTSlideMaster getXmlObject() {
		return _slide;
	}

    @Override
    protected String getRootElementName(){
        return "sldMaster";
    }

    @Override
    public XSLFSlideMaster getMasterSheet() {
        return null;
    }

    private Map<String, XSLFSlideLayout> getLayouts(){
        if(_layouts == null){
            _layouts = new HashMap<String, XSLFSlideLayout>();
            for (POIXMLDocumentPart p : getRelations()) {
                if (p instanceof XSLFSlideLayout){
                    XSLFSlideLayout layout = (XSLFSlideLayout)p;
                    _layouts.put(layout.getName().toLowerCase(Locale.ROOT), layout);
                }
            }
        }
        return _layouts;
    }

    /**
     *
     * @return all slide layouts referencing this master
     */
    public XSLFSlideLayout[] getSlideLayouts() {
        return getLayouts().values().toArray(new XSLFSlideLayout[_layouts.size()]);
    }

    /**
     * Get the slide layout by type.
     *
     * @param  type     The layout type. Cannot be null.
     *
     * @return the layout found or null on failure
     */
    public XSLFSlideLayout getLayout(SlideLayout type){
        for(XSLFSlideLayout layout : getLayouts().values()){
            if(layout.getType() == type) {
                return layout;
            }
        }
        return null;
    }

    /**
     * Get the slide layout by name.
     *
     * @param name  The layout name (case-insensitive). Cannot be null.
     *
     * @return the layout found or null on failure
     */
    public XSLFSlideLayout getLayout(String name) {
        return getLayouts().get(name.toLowerCase(Locale.ROOT));
    }


    @Override
    public XSLFTheme getTheme(){
        if(_theme == null){
            for (POIXMLDocumentPart p : getRelations()) {
                if (p instanceof XSLFTheme){
                    _theme = (XSLFTheme)p;
                    CTColorMapping cmap = _slide.getClrMap();
                    if(cmap != null){
                        _theme.initColorMap(cmap);
                    }
                    break;
                }
            }
        }
        return _theme;
    }

    protected CTTextListStyle getTextProperties(Placeholder textType) {
        CTTextListStyle props;
        CTSlideMasterTextStyles txStyles = getXmlObject().getTxStyles();
        switch (textType){
            case TITLE:
            case CENTERED_TITLE:
            case SUBTITLE:
                props = txStyles.getTitleStyle();
                break;
            case BODY:
                props = txStyles.getBodyStyle();
                break;
            default:
                props = txStyles.getOtherStyle();
                break;
        }
        return props;
    }

    /**
     * Render this sheet into the supplied graphics object
     *
     */
    @Override
    protected boolean canDraw(XSLFShape shape){
        if(shape instanceof XSLFSimpleShape){
            XSLFSimpleShape txt = (XSLFSimpleShape)shape;
            CTPlaceholder ph = txt.getCTPlaceholder();
            if(ph != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public XSLFBackground getBackground() {
        CTBackground bg = _slide.getCSld().getBg();
        if(bg != null) {
            return new XSLFBackground(bg, this);
        } else {
            return null;
        }
    }
}