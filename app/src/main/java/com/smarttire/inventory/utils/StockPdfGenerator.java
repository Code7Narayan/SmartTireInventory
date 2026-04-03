// FILE: utils/StockPdfGenerator.java  (FINAL — customer receipt shows paid/remaining per transaction)
package com.smarttire.inventory.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.smarttire.inventory.models.Customer;
import com.smarttire.inventory.models.Product;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StockPdfGenerator {
    private final Context context;
    private static final int W=595,H=842,MARGIN=36;
    private static final int C_PRIMARY=0xFFD32F2F,C_DARK=0xFFB71C1C,C_TEXT=0xFF212121;
    private static final int C_SECONDARY=0xFF757575,C_DIVIDER=0xFFE0E0E0;
    private static final int C_SUCCESS=0xFF388E3C,C_WARNING=0xFFE65100,C_BG=0xFFFAFAFA,C_WHITE=0xFFFFFFFF;

    public StockPdfGenerator(Context context){this.context=context;}

    // 1. STOCK DETAIL
    public File generateStockDetailPdf(String company,String model,String tireType,String tireSize,
                                       double price,int added,int sold,int stock,List<String[]> tx){
        PdfDocument doc=new PdfDocument();
        PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,1).create());
        Canvas c=pg.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        NumberFormat fmt=NumberFormat.getCurrencyInstance(new Locale("en","IN"));
        String now=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.getDefault()).format(new Date());
        int y=drawHeader(c,p,"STOCK DETAIL REPORT",now);
        y=drawSectionTitle(c,p,"Product Information",y);
        y=drawLV(c,p,"Company",company,y);y=drawLV(c,p,"Model",model.isEmpty()?"—":model,y);
        y=drawLV(c,p,"Type",tireType,y);y=drawLV(c,p,"Size",tireSize,y);
        y=drawLV(c,p,"Price",fmt.format(price),y);y+=12;
        y=drawSectionTitle(c,p,"Stock Summary",y);
        int cw=(W-MARGIN*2)/3;
        drawStat(c,p,MARGIN,y,cw-4,"Total Added",String.valueOf(added),C_SUCCESS);
        drawStat(c,p,MARGIN+cw,y,cw-4,"Total Sold",String.valueOf(sold),C_WARNING);
        drawStat(c,p,MARGIN+cw*2,y,cw-4,"In Stock",String.valueOf(stock),stock<5?C_WARNING:C_SUCCESS);
        y+=70;
        if(tx!=null&&!tx.isEmpty()){y+=8;y=drawSectionTitle(c,p,"Stock Movement History",y);
            y=drawTH(c,p,y,new String[]{"Type","Qty","Note","Date"},new int[]{10,8,55,24});
            for(String[] r:tx){if(y>H-80)break;boolean in="IN".equals(r[0]);
                p.setColor(in?C_SUCCESS:C_PRIMARY);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(9.5f);p.setTextAlign(Paint.Align.LEFT);
                c.drawText(in?"▲ IN":"▼ OUT",MARGIN+4,y,p);
                p.setColor(C_TEXT);p.setTypeface(Typeface.DEFAULT);p.setTextSize(9f);
                c.drawText(r[1],MARGIN+68,y,p);drawTr(c,p,r[2],MARGIN+105,y,240);
                p.setColor(C_SECONDARY);p.setTextAlign(Paint.Align.RIGHT);c.drawText(r[3],W-MARGIN,y,p);
                p.setColor(C_DIVIDER);p.setStrokeWidth(0.5f);c.drawLine(MARGIN,y+4,W-MARGIN,y+4,p);y+=18;}}
        drawFooter(c,p);doc.finishPage(pg);
        File f=save(doc,"StockDetail_"+company.replaceAll("\\s+","_"));doc.close();return f;}

    // 2. ALL STOCK
    public File generateAllStockPdf(List<Product> products){
        PdfDocument doc=new PdfDocument();
        NumberFormat fmt=NumberFormat.getCurrencyInstance(new Locale("en","IN"));
        String now=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.getDefault()).format(new Date());
        int pp=28,pages=Math.max(1,(int)Math.ceil(products.size()/(double)pp));
        for(int pg=0;pg<pages;pg++){
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,pg+1).create());
            Canvas c=page.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            int y=drawHeader(c,p,"STOCK INVENTORY REPORT",now);
            p.setTextAlign(Paint.Align.RIGHT);p.setColor(C_SECONDARY);p.setTextSize(9f);
            c.drawText("Page "+(pg+1)+"/"+pages,W-MARGIN,y-10,p);
            y=drawTH(c,p,y,new String[]{"Company","Model","Type","Size","Price","Qty"},new int[]{17,15,12,15,14,8});
            int st=pg*pp,en=Math.min(st+pp,products.size());
            for(int i=st;i<en;i++){Product pr=products.get(i);boolean lo=pr.getQuantity()<5;
                if(lo){p.setColor(Color.parseColor("#FFEBEE"));c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p);}
                int[]cx={MARGIN+4,MARGIN+105,MARGIN+195,MARGIN+265,MARGIN+365,MARGIN+445};
                String[]vs={pr.getCompanyName(),pr.getModelName().isEmpty()?"—":pr.getModelName(),
                        pr.getTireType(),pr.getTireSize(),fmt.format(pr.getPrice()),String.valueOf(pr.getQuantity())};
                for(int col=0;col<vs.length;col++){p.setColor(col==5&&lo?C_WARNING:C_TEXT);
                    p.setTypeface(col==5&&lo?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);p.setTextSize(9f);p.setTextAlign(Paint.Align.LEFT);
                    drawTr(c,p,vs[col],cx[col],y,90);}
                p.setColor(C_DIVIDER);p.setStrokeWidth(0.5f);c.drawLine(MARGIN,y+5,W-MARGIN,y+5,p);y+=18;}
            if(pg==pages-1){y+=10;p.setColor(C_PRIMARY);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(10f);p.setTextAlign(Paint.Align.LEFT);
                int qty=0,lo=0;for(Product pr:products){qty+=pr.getQuantity();if(pr.getQuantity()<5)lo++;}
                c.drawText("Total: "+products.size()+" products | Stock: "+qty+" | Low: "+lo,MARGIN,y,p);}
            drawFooter(c,p);doc.finishPage(page);}
        File f=save(doc,"AllStock_"+new SimpleDateFormat("yyyyMMdd",Locale.getDefault()).format(new Date()));doc.close();return f;}

    // 3. CUSTOMER RECEIPT — paid/remaining per transaction (Task 4 upgrade)
    public File generateCustomerReportPdf(Customer cu,double total,double paid,double due,
                                          List<String[]> sales,List<String[]> payments){
        PdfDocument doc=new PdfDocument();
        PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,1).create());
        Canvas c=pg.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        NumberFormat fmt=NumberFormat.getCurrencyInstance(new Locale("en","IN"));
        String now=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.getDefault()).format(new Date());
        int y=drawHeader(c,p,"CUSTOMER ACCOUNT STATEMENT",now);
        y=drawSectionTitle(c,p,"Customer Details",y);
        y=drawLV(c,p,"Name",cu.getName(),y);y=drawLV(c,p,"Phone",cu.getPhone(),y);
        if(cu.getAddress()!=null&&!cu.getAddress().isEmpty()&&!"—".equals(cu.getAddress()))y=drawLV(c,p,"Address",cu.getAddress(),y);
        if(cu.getGstNumber()!=null&&!cu.getGstNumber().isEmpty())y=drawLV(c,p,"GST",cu.getGstNumber(),y);
        y+=12;
        y=drawSectionTitle(c,p,"Account Summary",y);
        int cw=(W-MARGIN*2)/3;
        drawStat(c,p,MARGIN,y,cw-4,"Total Purchase",fmt.format(total),C_TEXT);
        drawStat(c,p,MARGIN+cw,y,cw-4,"Total Paid",fmt.format(paid),C_SUCCESS);
        drawStat(c,p,MARGIN+cw*2,y,cw-4,"Balance Due",fmt.format(due),due>0?C_WARNING:C_SUCCESS);
        y+=72;
        if(sales!=null&&!sales.isEmpty()){
            y=drawSectionTitle(c,p,"Purchase History",y);
            y=drawTH(c,p,y,new String[]{"Date","Product","Qty","Total","Paid","Due","St."},new int[]{14,30,5,13,13,12,10});
            for(int i=0;i<sales.size()&&y<H-110;i++){String[]row=sales.get(i);
                String st=row.length>6?row[6]:"paid";boolean hd=row.length>5&&!row[5].contains("0.00")&&!row[5].equals("₹0");
                if(i%2==0){p.setColor(C_BG);c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p);}
                p.setColor(C_SECONDARY);p.setTypeface(Typeface.DEFAULT);p.setTextSize(8f);p.setTextAlign(Paint.Align.LEFT);
                c.drawText(row[0],MARGIN+4,y,p);p.setColor(C_TEXT);drawTr(c,p,row[1],MARGIN+85,y,145);
                c.drawText(row[2],MARGIN+245,y,p);
                p.setColor(C_TEXT);c.drawText(row[3],MARGIN+270,y,p);
                p.setColor(C_SUCCESS);c.drawText(row.length>4?row[4]:"—",MARGIN+346,y,p);
                p.setColor(hd?C_WARNING:C_SUCCESS);p.setTypeface(hd?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
                c.drawText(row.length>5?row[5]:"₹0",MARGIN+418,y,p);
                p.setColor("partial".equals(st)?C_WARNING:"unpaid".equals(st)?0xFFC62828:C_SUCCESS);
                p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(7.5f);
                c.drawText(st.substring(0,Math.min(4,st.length())).toUpperCase(),MARGIN+490,y,p);
                p.setColor(C_DIVIDER);p.setStrokeWidth(0.5f);c.drawLine(MARGIN,y+4,W-MARGIN,y+4,p);y+=17;}}
        if(payments!=null&&!payments.isEmpty()&&y<H-100){y+=8;
            y=drawSectionTitle(c,p,"Payment History",y);
            y=drawTH(c,p,y,new String[]{"Date","Amount","Mode","Note"},new int[]{20,20,18,40});
            for(int i=0;i<payments.size()&&y<H-90;i++){String[]row=payments.get(i);
                if(i%2==0){p.setColor(C_BG);c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p);}
                p.setColor(C_SECONDARY);p.setTypeface(Typeface.DEFAULT);p.setTextSize(8.5f);p.setTextAlign(Paint.Align.LEFT);
                c.drawText(row[0],MARGIN+4,y,p);p.setColor(C_SUCCESS);c.drawText(row[1],MARGIN+118,y,p);
                p.setColor(C_TEXT);c.drawText(row[2],MARGIN+236,y,p);
                p.setColor(C_SECONDARY);drawTr(c,p,row.length>3?row[3]:"—",MARGIN+344,y,170);
                p.setColor(C_DIVIDER);p.setStrokeWidth(0.5f);c.drawLine(MARGIN,y+4,W-MARGIN,y+4,p);y+=16;}}
        if(y<H-80){y+=10;p.setColor(C_DARK);c.drawRect(MARGIN,y-13,W-MARGIN,y+6,p);
            p.setColor(C_WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(10f);p.setTextAlign(Paint.Align.LEFT);
            c.drawText("Balance Due: "+fmt.format(due),MARGIN+8,y,p);
            p.setTextAlign(Paint.Align.RIGHT);c.drawText(due>0?"OUTSTANDING":"FULLY PAID",W-MARGIN-8,y,p);}
        drawFooter(c,p);doc.finishPage(pg);
        File f=save(doc,"Receipt_"+cu.getName().replaceAll("\\s+","_"));doc.close();return f;}

    // 4. ALL CUSTOMERS
    public File generateAllCustomersPdf(List<Customer> customers){
        PdfDocument doc=new PdfDocument();NumberFormat fmt=NumberFormat.getCurrencyInstance(new Locale("en","IN"));
        String now=new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.getDefault()).format(new Date());
        int pp=30,pages=Math.max(1,(int)Math.ceil(customers.size()/(double)pp));
        for(int pg=0;pg<pages;pg++){
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(W,H,pg+1).create());
            Canvas c=page.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            int y=drawHeader(c,p,"ALL CUSTOMERS REPORT",now);
            p.setTextAlign(Paint.Align.RIGHT);p.setColor(C_SECONDARY);p.setTextSize(9f);c.drawText("Page "+(pg+1)+"/"+pages,W-MARGIN,y-10,p);
            y=drawTH(c,p,y,new String[]{"Name","Phone","Total","Paid","Due"},new int[]{25,18,19,18,17});
            int st=pg*pp,en=Math.min(st+pp,customers.size());double grandDue=0;
            for(int i=st;i<en;i++){Customer cu=customers.get(i);grandDue+=cu.getTotalDue();boolean hd=cu.getTotalDue()>0;
                if(hd){p.setColor(Color.parseColor("#FFF3E0"));c.drawRect(MARGIN,y-11,W-MARGIN,y+5,p);}
                p.setTextSize(9f);p.setTextAlign(Paint.Align.LEFT);p.setColor(C_TEXT);p.setTypeface(Typeface.DEFAULT);
                drawTr(c,p,cu.getName(),MARGIN+4,y,120);c.drawText(cu.getPhone(),MARGIN+135,y,p);
                c.drawText(fmt.format(cu.getTotalAmount()),MARGIN+245,y,p);
                p.setColor(C_SUCCESS);c.drawText(fmt.format(cu.getTotalPaid()),MARGIN+357,y,p);
                p.setColor(hd?C_WARNING:C_SUCCESS);p.setTypeface(hd?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
                c.drawText(fmt.format(cu.getTotalDue()),MARGIN+452,y,p);p.setTypeface(Typeface.DEFAULT);
                p.setColor(C_DIVIDER);p.setStrokeWidth(0.5f);c.drawLine(MARGIN,y+4,W-MARGIN,y+4,p);y+=18;}
            if(pg==pages-1){y+=10;p.setColor(C_PRIMARY);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(10f);p.setTextAlign(Paint.Align.LEFT);
                c.drawText("Total: "+customers.size()+" customers",MARGIN,y,p);
                p.setColor(C_WARNING);c.drawText("  |  Grand Total Due: "+fmt.format(grandDue),MARGIN+160,y,p);}
            drawFooter(c,p);doc.finishPage(page);}
        File f=save(doc,"AllCustomers_"+new SimpleDateFormat("yyyyMMdd",Locale.getDefault()).format(new Date()));doc.close();return f;}

    // Helpers
    private int drawHeader(Canvas c,Paint p,String title,String date){
        p.setColor(C_PRIMARY);c.drawRect(0,0,W,88,p);
        p.setColor(C_WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(20f);p.setTextAlign(Paint.Align.CENTER);
        c.drawText("ANAND TIRE INVENTORY",W/2f,34,p);
        p.setTypeface(Typeface.DEFAULT);p.setTextSize(9f);c.drawText("Pune Banglore Highway, Kolhapur Naka, Karad  |  Mob: 9860808003",W/2f,52,p);
        int y=100;p.setColor(C_TEXT);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(15f);c.drawText(title,W/2f,y,p);
        p.setColor(C_PRIMARY);p.setStrokeWidth(2);c.drawLine(MARGIN,y+6,W-MARGIN,y+6,p);
        p.setColor(C_SECONDARY);p.setTypeface(Typeface.DEFAULT);p.setTextSize(9f);p.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Generated: "+date,W-MARGIN,y+20,p);return y+34;}
    private int drawSectionTitle(Canvas c,Paint p,String t,int y){y+=8;p.setColor(Color.parseColor("#FFEBEE"));c.drawRect(MARGIN,y-13,W-MARGIN,y+5,p);p.setColor(C_PRIMARY);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(10.5f);p.setTextAlign(Paint.Align.LEFT);c.drawText(t,MARGIN+6,y,p);return y+16;}
    private int drawLV(Canvas c,Paint p,String l,String v,int y){p.setColor(C_SECONDARY);p.setTypeface(Typeface.DEFAULT);p.setTextSize(10f);p.setTextAlign(Paint.Align.LEFT);c.drawText(l+":",MARGIN+8,y,p);p.setColor(C_TEXT);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(v,MARGIN+100,y,p);return y+18;}
    private void drawStat(Canvas c,Paint p,int x,int y,int w,String l,String v,int vc){p.setColor(Color.parseColor("#F5F5F5"));p.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x,y,x+w,y+55),6,6,p);p.setColor(vc);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(14f);p.setTextAlign(Paint.Align.CENTER);c.drawText(v,x+w/2f,y+28,p);p.setColor(C_SECONDARY);p.setTypeface(Typeface.DEFAULT);p.setTextSize(9f);c.drawText(l,x+w/2f,y+46,p);p.setStyle(Paint.Style.FILL);}
    private int drawTH(Canvas c,Paint p,int y,String[]hs,int[]pcts){p.setColor(C_DARK);c.drawRect(MARGIN,y-13,W-MARGIN,y+6,p);p.setColor(C_WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(9.5f);p.setTextAlign(Paint.Align.LEFT);int avail=W-MARGIN*2,xPos=MARGIN+4;for(int i=0;i<hs.length;i++){c.drawText(hs[i],xPos,y,p);xPos+=avail*pcts[i]/100;}return y+18;}
    private void drawTr(Canvas c,Paint p,String t,int x,int y,int mw){if(t==null)t="—";p.setTextAlign(Paint.Align.LEFT);if(p.measureText(t)<=mw){c.drawText(t,x,y,p);return;}while(p.measureText(t+"…")>mw&&t.length()>1)t=t.substring(0,t.length()-1);c.drawText(t+"…",x,y,p);}
    private void drawFooter(Canvas c,Paint p){p.setColor(C_DIVIDER);p.setStrokeWidth(1);c.drawLine(MARGIN,H-50,W-MARGIN,H-50,p);p.setColor(C_PRIMARY);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(10f);p.setTextAlign(Paint.Align.CENTER);c.drawText("Thank you — Anand Tire Inventory",W/2f,H-33,p);p.setColor(C_SECONDARY);p.setTypeface(Typeface.DEFAULT);p.setTextSize(8f);c.drawText("System-generated document.",W/2f,H-18,p);}
    private File save(PdfDocument doc,String name){String fn=name+"_"+System.currentTimeMillis()+".pdf";File dir;if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.Q){dir=new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"Invoices");}else{dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"SmartTire/Invoices");}if(!dir.exists())dir.mkdirs();File file=new File(dir,fn);try(FileOutputStream fos=new FileOutputStream(file)){doc.writeTo(fos);return file;}catch(Exception e){e.printStackTrace();return null;}}
    public void openPdf(File file){if(file==null||!file.exists()){Toast.makeText(context,"PDF not found",Toast.LENGTH_SHORT).show();return;}try{Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".provider",file);Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(i);}catch(Exception e){Toast.makeText(context,"No PDF viewer",Toast.LENGTH_SHORT).show();}}
    public void sharePdf(File file,String subject){if(file==null||!file.exists())return;try{Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".provider",file);Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,uri);i.putExtra(Intent.EXTRA_SUBJECT,subject);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);context.startActivity(Intent.createChooser(i,"Share Report"));}catch(Exception e){Toast.makeText(context,"Error sharing",Toast.LENGTH_SHORT).show();}}
}