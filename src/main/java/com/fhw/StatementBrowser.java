package com.fhw;

import java.util.Date;
import java.util.List;
import javax.inject.Named;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.*;
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
import lombok.*;

@Data
@Named
@SessionScoped
public class StatementBrowser
        implements Serializable
{

    private Date date;
    private Integer customerId;
    private List<Integer> customers;
    private List<SelectItem> statements;
    
    
    
    private static final String keyspace = "statementarchive";
    private static final String table = "statements";


    public StatementBrowser()
    {

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
            
            Statement q = QueryBuilder.select().column("customer_id").from(keyspace, table).allowFiltering().limit(300).where(eq("year", y)).and(eq("month", m)).and(eq("day", day));
            ResultSet rs = s.execute(q);
            Map<Integer, Integer> map = new HashMap<>();
            for (Row row : rs)
            {
                Integer b = row.getInt("customer_id");
                map.put(b, b);
            }
            customers = new ArrayList<>();
            for (Integer i : map.keySet())
            {
                customers.add(i);
            }
        }
        finally
        {
            close(c, s);
        }
    }

    public void handleCustomerSelect()
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
            Statement q = QueryBuilder.select().column("statement_archive_id").column("description").from(keyspace, table).allowFiltering().limit(300).where(eq("year", y)).and(eq("month", m)).and(eq("day", day)).and(eq("customer_id", customerId));
            ResultSet rs = s.execute(q);
            Map<Integer, Integer> map = new HashMap<>();
            statements = new ArrayList<>();
            for (Row row : rs)
            {
                UUID id = row.getUUID("statement_archive_id");
                String description = row.getString("description");
                statements.add(new SelectItem(id.toString(), description));
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
