package com.fhw;

import java.util.Date;
import java.util.List;
import javax.inject.Named;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.model.SelectItem;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@Named
@SessionScoped
public class ReportBrowser
        implements Serializable
{

    private Date date;
    private int bankId;
    private List<Integer> banks;
    private List<SelectItem> reports;

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

    public List<SelectItem> getReports()
    {
        return reports;
    }

    public void setReports(List<SelectItem> reports)
    {
        this.reports = reports;
    }

    public void handleDateSelect(SelectEvent event)
    {
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
            Statement q = QueryBuilder.select().column("bank_id").from("pinappsreportarchive", "reports").allowFiltering().limit(300).where(eq("year", y)).and(eq("month", m)).and(eq("day", day));
            ResultSet rs = s.execute(q);
            Map<Integer, Integer> map = new HashMap<>();
            banks = new ArrayList<>();
            for (Row row : rs)
            {
                Integer b = row.getInt("bank_id");
                map.put(b, b);
            }
            banks = new ArrayList<>();
            for (Integer i : map.keySet())
            {
                banks.add(i);
            }
        }
        finally
        {
            close(c, s);
        }
    }

    public void handleBankSelect()
    {
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
            Statement q = QueryBuilder.select().column("report_archive_id").column("description").from("pinappsreportarchive", "reports").allowFiltering().limit(300).where(eq("year", y)).and(eq("month", m)).and(eq("day", day)).and(eq("bank_id", bankId));
            ResultSet rs = s.execute(q);
            Map<Integer, Integer> map = new HashMap<>();
            reports = new ArrayList<>();
            for (Row row : rs)
            {
                UUID id = row.getUUID("report_archive_id");
                String description = row.getString("description");
                reports.add(new SelectItem(id.toString(), description));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            close(c, s);
        }
    }

    public StreamedContent downLoad()
    {
        StreamedContent sc = null;
        FacesContext fc = FacesContext.getCurrentInstance();
        if (PhaseId.RENDER_RESPONSE == fc.getCurrentPhaseId())
        {
            sc = new DefaultStreamedContent();
        }
        else
        {
            Cluster c = null;
            Session s = null;
            String id;
            try
            {
                Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
                id = (String) params.get("ruid");
                System.out.println("my ruid is " + id);
                c = connect();
                s = c.connect();
                UUID uuid = UUID.fromString(id);
                Statement q = QueryBuilder.select().column("report").column("description").from("pinappsreportarchive", "reports").where(eq("report_archive_id", uuid));
                ResultSet rs = s.execute(q);
                Row one = rs.one();
                String filename = one.getString("description");
                ByteBuffer bb = one.getBytes("report");
                sc = new DefaultStreamedContent(new ByteBufferBackedInputStream(bb), "application/octet-stream", filename);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                close(c, s);
            }
        }
        return (sc);
    }

    private Cluster connect()
    {
        return (Cluster.builder().addContactPoint("localhost").build());
    }

    private void close(Cluster c, Session s)
    {
        if (null != s)
        {
            s.close();
        }
        if (null != c)
        {
            c.close();
        }
    }

    public static final class ByteBufferBackedInputStream extends InputStream
    {

        ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf)
        {
            this.buf = buf;
        }

        @Override
        public int read() throws IOException
        {
            if (!buf.hasRemaining())
            {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        @Override
        public int read(byte[] bytes, int off, int len)
                throws IOException
        {
            if (!buf.hasRemaining())
            {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }

}
