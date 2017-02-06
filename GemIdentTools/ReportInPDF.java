/*
    GemIdent v1.1b
    Interactive Image Segmentation Software via Supervised Statistical Learning
    http://gemident.com

    Copyright (C) 2009 Professor Susan Holmes & Adam Kapelner, Stanford University

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details:

    http://www.gnu.org/licenses/gpl-2.0.txt

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package GemIdentTools;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import GemIdentAnalysisConsole.ConsoleParser;
import GemIdentOperations.Run;

/**
 * Provides the PDF generation for the "report" function in the data analysis page.
 * Runs on top of iText - an open source PDF library for Java.
 *
 * @author Adam Kapelner
 *
 * @see <a href="http://www.lowagie.com/iText/">iText</a>
 */
public class ReportInPDF {

	/** the width of the thumbnail of the global image in the first page of the report */
	public static final float thumbWidth=1500;

	/** the mapping from function calls to their results */
	private LinkedHashMap<String,Object> calls;
	/** the mapping from the phenotypes to their total counts from a classification */
	private LinkedHashMap<String,Long> totalCounts;
	/** the mapping from the phenotypes to their type I error rates from a classification */
	private LinkedHashMap<String,Double> errorRates;
	/** the {@link com.lowagie.text.pdf.PdfWriter PdfWriter} object that will write to the report pdf */
	private PdfWriter writer;
	/** the {@link com.lowagie.text.Document Document} object that will write to the report pdf */
	private Document document;
	/** the font of the subtitles in the report */
	private Font subTitleFont;
	/** the {@link com.lowagie.text.pdf.PdfPTable table} on the first page of the report */
	private PdfPTable table;

	/**
	 * Creates a PDF report and then auto-opens it in the operating system environment
	 *
	 * @param totalCounts		the mapping from phenotypes to their counts
	 * @param errorRates		the mapping from phenotypes to their errors
	 * @param calls				the mapping from user function calls to their results
	 * @param t					the timestamp when the report function was called
	 * @throws Exception		if there's an error in PDF creation
	 */
	public ReportInPDF(LinkedHashMap<String,Long> totalCounts,LinkedHashMap<String,Double> errorRates,LinkedHashMap<String,Object> calls,Timestamp t) throws Exception{
		this.totalCounts=totalCounts;
		this.errorRates=errorRates;
//		this.calls=SelectRelevantCalls(calls);
		this.calls=calls;

		CreatePDFLayoutAndContent(t);

		IOTools.RunProgramInOS(ConsoleParser.dumpFilenameOutput("report","pdf",t));
	}

	/**
	 * Does the layout of the PDF and then adds the content
	 *
	 * @param t				the timestamp
	 * @throws Exception	if there's an error in PDF creation
	 */
	private void CreatePDFLayoutAndContent(Timestamp t) throws Exception {
		document=new Document(PageSize.LETTER, 50, 50, 50, 50);
		writer=PdfWriter.getInstance(document,new FileOutputStream(ConsoleParser.dumpFilenameOutputFull("report","pdf",t)));
		writer.setStrictImageSequence(true);
		document.open();

		subTitleFont=FontFactory.getFont(FontFactory.TIMES,18,Font.BOLD,Color.BLACK);

		float[] rel={.666f,.334f};
		table = new PdfPTable(rel);
		table.getDefaultCell().setBorder(0);
		table.setHorizontalAlignment(0);
		table.setWidthPercentage(100f);
//		table.setLockedWidth(true);

		AddTitle();
		document.add(new Phrase("\n"));
		AddThumbnailImage();
		AddSummary();
		document.add(table);
		AddCalls();

		document.close();
	}
	/** adds the title (the project name to the PDF report */
	private void AddTitle() throws Exception {
		Font titleFont=FontFactory.getFont(FontFactory.TIMES,36,Font.BOLD,Color.BLACK);
		Paragraph title=new Paragraph(Run.it.projectName+"\n",titleFont);
		document.add(title);
	}
	/** adds the thumbnail of the global image to the PDF report */
	private void AddThumbnailImage() throws Exception {
		//"getGlobalThumbnailImage" returns a BufferedImage of width thumbWidth
		BufferedImage I=Run.it.imageset.getGlobalThumbnailImage(thumbWidth);
		Image image=Image.getInstance(I,null);
//		PdfPCell cell=new PdfPCell();
//		cell.setMinimumHeight(image.get);
//		cell.set

//		cell.addElement(image);
		table.addCell(image);
	}
	/** adds the counts and error rates summary on the right of the thumbnail */
	private void AddSummary() throws Exception {

		Font elementFont=FontFactory.getFont(FontFactory.COURIER,12,Font.NORMAL,Color.BLACK);
		elementFont.leading(0);

		NumberFormat format=NumberFormat.getInstance();
		format.setMinimumFractionDigits(2);
		format.setMaximumFractionDigits(2);

		PdfPTable small=new PdfPTable(1);

		if (totalCounts != null && errorRates != null){
			Paragraph subTitleCounts=new Paragraph(subTitleFont.leading(1f),"Counts\n",subTitleFont);
			String counts="";
			for (String phenotype:totalCounts.keySet())
				counts+="\t"+phenotype+": "+totalCounts.get(phenotype)+"\n";
			Phrase countsPhrase=new Phrase(elementFont.leading(.75f),counts+"\n",elementFont);
			subTitleCounts.add(countsPhrase);
			Paragraph subTitleErrors=new Paragraph(subTitleFont.leading(1f),"Error Rates\n",subTitleFont);
			String errors="";
			for (String phenotype:errorRates.keySet()){
				Double errorRate=errorRates.get(phenotype);
				String errorString;
				if (errorRate == null)
					continue;
				else {
					errorString=format.format(errorRate)+"%";
					if (!errorString.equals("%")) //best I could do here
						errors+="\t"+phenotype+": "+errorString+"\n";
				}
			}
			subTitleErrors.add(new Phrase(elementFont.leading(.75f),errors+"\n",elementFont));

			PdfPCell cell=new PdfPCell();
			cell.setBorder(0);
			cell.addElement(subTitleCounts);
			cell.addElement(subTitleErrors);
			small.addCell(cell);
		}

		table.addCell(small);
//		document.add(small);
	}
	/** adds a transcript of the function calls and their results (the meat of the report) */
	private void AddCalls() throws Exception {

		Font callFont=FontFactory.getFont(FontFactory.COURIER,14,Font.NORMAL,Color.BLACK);
		Font outputFont=FontFactory.getFont(FontFactory.COURIER,12,Font.NORMAL,new Color(0,144,0));

		if (calls != null){
			if (calls.size() > 0){
				document.add(new Paragraph(subTitleFont.leading(1f),"Analysis\n\n",subTitleFont));
				for (String call:calls.keySet()){
					document.add(new Phrase(callFont.leading(1f),"\t"+call+"\n",callFont));

					Object returnValue=calls.get(call);
					if (returnValue instanceof String)
						document.add(new Phrase(outputFont.leading(1f),"\t\t"+(String)returnValue+"\n",outputFont));
					else if (returnValue instanceof BufferedImage){
//						float[] rel={.75f,.25f}; //just 75% of the area
						PdfPTable table = new PdfPTable(1);
						table.getDefaultCell().setBorder(0);
						table.setHorizontalAlignment(0);
						table.setWidthPercentage(75f);
						Image image=Image.getInstance((BufferedImage)returnValue,null);
						table.addCell(image);
						document.add(table);
					}
				}
			}
		}
	}
//	private LinkedHashMap<String,Object> SelectRelevantCalls(LinkedHashMap<String,Object> calls) {
////		LinkedHashMap<String,String> relevant=new LinkedHashMap<String,String>();
//		return calls;
////		return relevant;
//	}
}