package com.fhw;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import java.util.ArrayList;
import java.util.Calendar;
import org.primefaces.event.SelectEvent;

@Named
@RequestScoped
public class ReportBrowser
{
    private Date date; 
    private int bankId; 
    private List<Integer> banks; 
    private List<String> reports; 
    
    public ReportBrowser()
    {
        
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public int getBankId()
    {
        return bankId;
    }

    public void setBankId(int bankId)
    {
        this.bankId = bankId;
    }

    public List<Integer> getBanks()
    {
        return banks;
    }

    public void setBanks(List<Integer> banks)
    {
        this.banks = banks;
    }

    public List<String> getReports()
    {
        return reports;
    }

    public void setReports(List<String> reports)
    {
        this.reports = reports;
    }

    
    public void handleDateSelect(SelectEvent event)
    {
        Object o = event.getObject(); 
        Date d = (Date)o; 
        Cluster c = null; 
        Session s = null; 
        try
        {
            c = connect(); 
            s = c.connect();
            int y, m, day; 
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            y = cal.get(Calendar.YEAR); 
            m = cal.get(Calendar.MONTH); 
            day = cal.get(Calendar.DAY_OF_MONTH);            
            Statement q = QueryBuilder.select().distinct().column("bank_id").from("pinappsreportarchive", "reports").where(eq("year", y)).and(eq("month", m)).and(eq("day",day));
            ResultSet rs = s.execute(q); 
            banks = new ArrayList<>();
            for (Row row : rs)
            {
                banks.add(row.getInt("bank_id"));
            }                        
        }
        finally
        {
            close(c,s); 
        }                
    }

    
    private Cluster connect()
    {
        return (Cluster.builder().addContactPoint("localhost").build());        
    }            
    
    private void close(Cluster c, Session s)
    {
        if(null != s)
            s.shutdown(); 
        if(null != c)
            c.shutdown();         
    }
}
