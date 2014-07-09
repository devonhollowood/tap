/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2009.                            (c) 2009.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  $Revision: 4 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.tap.schema;

import ca.nrc.cadc.tap.TapPlugin;
import ca.nrc.cadc.uws.Job;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Given a DataSource to a TAP_SCHEMA, returns a TapSchema object containing the TAP_SCHEMA data.
 * The fully qualified names of tables in the tap_schema can be modified in a subclass as long 
 * as the change(s) are made before the get method is called (*TableName variables).
 */
public class TapSchemaDAO implements TapPlugin
{
    private static final Logger log = Logger.getLogger(TapSchemaDAO.class);

    // standard tap_schema table names
    protected String schemasTableName = "tap_schema.schemas";
    protected String tablesTableName = "tap_schema.tables";
    protected String columnsTableName = "tap_schema.columns";
    protected String keysTableName = "tap_schema.keys";
    protected String keyColumnsTableName = "tap_schema.key_columns";

    // SQL to select all rows from TAP_SCHEMA.schemas.
    protected String SELECT_SCHEMAS_COLS = "schema_name, description, utype";
    protected String orderSchemaClause = " ORDER BY schema_name";

    // SQL to select all rows from TAP_SCHEMA.tables.
    protected String SELECT_TABLES_COLS = "schema_name, table_name, description, utype";
    protected String orderTablesClause = " ORDER BY schema_name,table_name";

    // SQL to select all rows from TAP_SCHEMA.colums.
    protected String SELECT_COLUMNS_COLS = "table_name, column_name, description, utype, ucd, unit, datatype, size, principal, indexed, std, id";
    protected String orderColumnsClause = " ORDER BY table_name,column_name";
    
    // SQL to select all rows from TAP_SCHEMA.keys.
    protected String SELECT_KEYS_COLS = "key_id, from_table, target_table,description,utype";
    protected String orderKeysClause = " ORDER BY key_id,from_table,target_table";

    // SQL to select all rows from TAP_SCHEMA.key_columns.
    protected String SELECT_KEY_COLUMNS_COLS = "key_id, from_column, target_column";
    protected String orderKeyColumnsClause = " ORDER BY key_id, from_column, target_column";

    protected DataSource dataSource;
    protected boolean ordered;
    protected Job job;
    
    // Indicates function return datatype matches argument datatype.
    public static final String ARGUMENT_DATATYPE = "ARGUMENT_DATATYPE";

    /**
     * Construct a new TapSchemaDAO.
     * 
     * @param dataSource TAP_SCHEMA DataSource.
     */
    public TapSchemaDAO() { }

    public void setJob(Job job)
    {
        this.job = job;
    }
    
    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void setOrdered(boolean ordered)
    {
        this.ordered = ordered;
    }

    /**
     * Creates and returns a TapSchema object representing all of the data in TAP_SCHEMA.
     * 
     * @return TapSchema containing all of the data from TAP_SCHEMA.
     */
    public TapSchema get()
    {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        TapSchema tapSchema = new TapSchema();
        String sql, tab;

        // List of TAP_SCHEMA.schemas
        tab = schemasTableName;
        sql = "SELECT " + SELECT_SCHEMAS_COLS + " FROM " + tab;
        sql = appendWhere(tab, sql);
        if (ordered) sql += orderSchemaClause;
        log.debug(sql);
        tapSchema.schemaDescs = jdbc.query(sql, new SchemaMapper());

        // List of TAP_SCHEMA.tables
        tab = tablesTableName;
        sql = "SELECT " + SELECT_TABLES_COLS + " FROM " + tab;
        sql = appendWhere(tab, sql);
        if (ordered) sql += orderTablesClause;
        log.debug(sql);
        List<TableDesc> tableDescs = jdbc.query(sql, new TableMapper());

        // Add the Tables to the Schemas.
        addTablesToSchemas(tapSchema.schemaDescs, tableDescs);

        // List of TAP_SCHEMA.columns
        tab = columnsTableName;
        sql = "SELECT " + SELECT_COLUMNS_COLS + " FROM " + tab;
        sql = appendWhere(tab, sql);
        if (ordered) sql += orderColumnsClause;
        log.debug(sql);
        List<ColumnDesc> columnDescs = jdbc.query(sql, new ColumnMapper());

        // Add the Columns to the Tables.
        addColumnsToTables(tableDescs, columnDescs);

        // List of TAP_SCHEMA.keys
        tab = keysTableName;
        sql = "SELECT " + SELECT_KEYS_COLS + " FROM " + tab;
        sql = appendWhere(tab, sql);
        if (ordered) sql += orderKeysClause;
        log.debug(sql);
        List<KeyDesc> keyDescs = jdbc.query(sql, new KeyMapper());

        // List of TAP_SCHEMA.key_columns
        tab = keyColumnsTableName;
        sql = "SELECT " + SELECT_KEY_COLUMNS_COLS + " FROM " + tab;
        sql = appendWhere(tab, sql);
        if (ordered) sql += orderKeyColumnsClause;
        log.debug(sql);
        List<KeyColumnDesc> keyColumnDescs = jdbc.query(sql, new KeyColumnMapper());

        // Add the KeyColumns to the Keys.
        addKeyColumnsToKeys(keyDescs, keyColumnDescs);

        // connect foreign keys to the fromTable
        addForeignKeys(tapSchema, keyDescs);

        // Add the List of FunctionDescs.
        tapSchema.functionDescs = getFunctionDescs();
        
        for (SchemaDesc s : tapSchema.schemaDescs) 
        {
            int num = 0;
            if (s.tableDescs != null)
                num = s.tableDescs.size();
            log.debug("schema " + s.schemaName + " has " + num + " tables");
        }

        return tapSchema;
    }
    
    /**
     * Append a where clause to the query that selects from the specified table.
     * The default implementation does nothing (returns in the provided SQL as-is).
     * </p>
     * <p>
     * If you want to implement some additional conditions, such as having private records
     * only visible to certain authenticated and authorized users, you can append some
     * conditions (or re-write the query as long as the select-list is not altered) here.
     * 
     * @param sql
     * @return modified SQL
     */
    protected String appendWhere(String tapSchemaTablename, String sql)
    {
        return sql;
    }

    /**
     * Creates Lists of Tables with a common Schema name, then adds the Lists to the Schemas.
     * 
     * @param schemaDescs List of Schemas.
     * @param tableDescs List of Tables.
     */
    private void addTablesToSchemas(List<SchemaDesc> schemaDescs, List<TableDesc> tableDescs)
    {
        for (TableDesc tableDesc : tableDescs)
        {
            for (SchemaDesc schemaDesc : schemaDescs)
            {
                if (tableDesc.schemaName.equals(schemaDesc.schemaName))
                {
                    schemaDesc.tableDescs.add(tableDesc);
                    break;
                }
            }
        }
    }

    /**
     * Creates Lists of Columns with a common Table name, then adds the Lists to the Tables.
     * 
     * @param tableDescs List of Tables.
     * @param columnDescs List of Columns.
     */
    private void addColumnsToTables(List<TableDesc> tableDescs, List<ColumnDesc> columnDescs)
    {
        for (ColumnDesc col : columnDescs)
        {
            for (TableDesc tableDesc : tableDescs)
            {
                if (col.tableName.equals(tableDesc.tableName))
                {
                    tableDesc.columnDescs.add(col);
                    break;
                }
            }
        }
    }

    /**
     * Creates Lists of KeyColumns with a common Key keyID, then adds the Lists to the Keys.
     * 
     * @param keyDescs List of Keys.
     * @param keyColumnDescs List of KeyColumns.
     */
    private void addKeyColumnsToKeys(List<KeyDesc> keyDescs, List<KeyColumnDesc> keyColumnDescs)
    {
        for (KeyColumnDesc keyColumnDesc : keyColumnDescs)
        {
            for (KeyDesc keyDesc : keyDescs)
            {
                if (keyColumnDesc.keyId.equals(keyDesc.keyId))
                {
                    keyDesc.keyColumnDescs.add(keyColumnDesc);
                    break;
                }
            }
        }
    }

    /**
     * Adds foreign keys (KeyDesc) to the from table.
     * 
     * @param ts
     */
    private void addForeignKeys(TapSchema ts, List<KeyDesc> keyDescs)
    {
        for (KeyDesc key : keyDescs)
        {
            for (SchemaDesc sd : ts.schemaDescs)
            {
                for (TableDesc td : sd.tableDescs)
                {
                    if ( key.fromTable.equals(td.tableName))
                    {
                        td.keyDescs.add(key);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get white-list of supported functions. TAP implementors that want to allow
     * additiopnal functions to be used in queries to be used should override this
     * method, call <code>super.getFunctionDescs()</code>, and then add additional
     * FunctionDesc descriptors to the list before returning it.
     *
     * @return white list of allowed functions
     */
    protected List<FunctionDesc> getFunctionDescs()
    {
        List<FunctionDesc> functionDescs = new ArrayList<FunctionDesc>();

        // ADQL functions.
        functionDescs.add(new FunctionDesc("AREA", "deg**2", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("BOX", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("CENTROID", "", "adql:POINT"));
        functionDescs.add(new FunctionDesc("CIRCLE", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("CONTAINS", "", "adql:INTEGER"));
        functionDescs.add(new FunctionDesc("COORD1", "deg", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("COORD2", "deg", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("COORDSYS", "", "adql:VARCHAR"));
        functionDescs.add(new FunctionDesc("DISTANCE", "deg", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("INTERSECTS", "", "adql:INTEGER"));
        functionDescs.add(new FunctionDesc("POINT", "", "adql:POINT"));
        functionDescs.add(new FunctionDesc("POLYGON", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("REGION", "", "adql:REGION"));

        // ADQL reserved keywords that are functions.
        functionDescs.add(new FunctionDesc("ABS", ""));
        functionDescs.add(new FunctionDesc("ACOS", "radians", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("ASIN", "radians", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("ATAN", "radians", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("ATAN2", "radians", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("CEILING", "", "adql:INTEGER"));
        functionDescs.add(new FunctionDesc("COS", "radians", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("COT", "radians", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("DEGREES", "deg", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("EXP", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("FLOOR", "", "adql:INTEGER"));
        functionDescs.add(new FunctionDesc("LN", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("LOG", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("LOG10", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("MOD", ""));
        /*
         * Part of the ADQL BNF, but currently not parseable pending bug
         * fix in the jsqlparser.
         *
         * functionDescs.add(new FunctionDesc("PI", "", "adql:DOUBLE"));
         */
        functionDescs.add(new FunctionDesc("POWER", ""));
        functionDescs.add(new FunctionDesc("RADIANS", "radians", "adql:DOUBLE"));
        /*
         * Part of the ADQL BNF, but currently not parseable pending bug
         * fix in the jsqlparser.
         *
         * functionDescs.add(new FunctionDesc("RAND", "", "adql:DOUBLE"));
         */
        functionDescs.add(new FunctionDesc("ROUND", ""));
        functionDescs.add(new FunctionDesc("SIN", "radians", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("SQRT", ""));
        functionDescs.add(new FunctionDesc("TAN", "radians", "adql:DOUBLE"));
        /*
         * Part of the ADQL BNF, but currently not parseable.
         *
         * functionDescs.add(new FunctionDesc("TRUNCATE", "", "adql:DOUBLE"));
         */

        // SQL Aggregate functions.
        functionDescs.add(new FunctionDesc("AVG", ""));
        functionDescs.add(new FunctionDesc("COUNT", "", "adql:INTEGER"));
        functionDescs.add(new FunctionDesc("MAX", ""));
        functionDescs.add(new FunctionDesc("MIN", ""));
        functionDescs.add(new FunctionDesc("STDDEV", "", "adql:DOUBLE"));
        functionDescs.add(new FunctionDesc("SUM", ""));
        functionDescs.add(new FunctionDesc("VARIANCE", "", "adql:DOUBLE"));
        
        // SQL String functions.
//        functionDescs.add(new FunctionDesc("BIT_LENGTH", "", "adql:INTEGER"));
//        functionDescs.add(new FunctionDesc("CHARACTER_LENGTH", "", "adql:INTEGER"));
//        functionDescs.add(new FunctionDesc("LOWER", "", "adql:VARCHAR"));
//        functionDescs.add(new FunctionDesc("OCTET_LENGTH", "", "adql:INTEGER"));
//        functionDescs.add(new FunctionDesc("OVERLAY", "", "adql:VARCHAR")); //SQL92???
//        functionDescs.add(new FunctionDesc("POSITION", "", "adql:INTEGER"));
//        functionDescs.add(new FunctionDesc("SUBSTRING", "", "adql:VARCHAR"));
//        functionDescs.add(new FunctionDesc("TRIM", "", "adql:VARCHAR"));
//        functionDescs.add(new FunctionDesc("UPPER", "", "adql:VARCHAR"));

        // SQL Date functions.
//        functionDescs.add(new FunctionDesc("CURRENT_DATE", "", "adql:TIMESTAMP"));
//        functionDescs.add(new FunctionDesc("CURRENT_TIME", "", "adql:TIMESTAMP"));
//        functionDescs.add(new FunctionDesc("CURRENT_TIMESTAMP", "", "adql:TIMESTAMP"));
//        functionDescs.add(new FunctionDesc("EXTRACT", "", "adql:TIMESTAMPs"));
//        functionDescs.add(new FunctionDesc("LOCAL_DATE", "", "adql:TIMESTAMP"));   //SQL92???
//        functionDescs.add(new FunctionDesc("LOCAL_TIME", "", "adql:TIMESTAMP"));   //SQL92???
//        functionDescs.add(new FunctionDesc("LOCAL_TIMESTAMP", "", "adql:TIMESTAMP"));  //SQL92???

        

//        functionDescs.add(new FunctionDesc("BETWEEN", ""));
//        functionDescs.add(new FunctionDesc("CASE", ""));
//        functionDescs.add(new FunctionDesc("CAST", ""));
//        functionDescs.add(new FunctionDesc("COALESCE", ""));
//        functionDescs.add(new FunctionDesc("CONVERT", ""));
//        functionDescs.add(new FunctionDesc("TRANSLATE", ""));
        
        // Sub-selects
//        functionDescs.add(new FunctionDesc("ALL", ""));
//        functionDescs.add(new FunctionDesc("ANY", ""));
//        functionDescs.add(new FunctionDesc("EXISTS", ""));
//        functionDescs.add(new FunctionDesc("IN", ""));

        return functionDescs;
    }

    /**
     * Creates a List of Schema populated from the ResultSet.
     */
    private static final class SchemaMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            SchemaDesc schemaDesc = new SchemaDesc();
            schemaDesc.schemaName = rs.getString("schema_name");
            schemaDesc.description = rs.getString("description");
            schemaDesc.utype = rs.getString("utype");
            schemaDesc.tableDescs = new ArrayList<TableDesc>();
            //log.debug("found: " + schemaDesc);
            return schemaDesc;
        }
    }

    /**
     * Creates a List of Table populated from the ResultSet.
     */
    private static final class TableMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            TableDesc tableDesc = new TableDesc();
            tableDesc.schemaName = rs.getString("schema_name");
            tableDesc.tableName = rs.getString("table_name");
            tableDesc.description = rs.getString("description");
            tableDesc.utype = rs.getString("utype");
            tableDesc.columnDescs = new ArrayList<ColumnDesc>();
            tableDesc.keyDescs = new ArrayList<KeyDesc>();
            //log.debug("found: " + tableDesc);
            return tableDesc;
        }
    }

    /**
     * Creates a List of Column populated from the ResultSet.
     */
    private static final class ColumnMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            ColumnDesc col = new ColumnDesc();
            col.tableName = rs.getString("table_name");
            col.columnName = rs.getString("column_name");            
            col.description = rs.getString("description");
            col.utype = rs.getString("utype");
            col.ucd = rs.getString("ucd");
            col.unit = rs.getString("unit");
            col.datatype = rs.getString("datatype");
            col.size = rs.getObject("size") == null ? null : Integer.valueOf(rs.getInt("size"));
            col.principal = intToBoolean(rs.getInt("principal"));
            col.indexed = intToBoolean(rs.getInt("indexed"));
            col.std = intToBoolean(rs.getInt("std"));
            col.id = rs.getString("id");
            //log.debug("found: " + col);
            return col;
        }

        private boolean intToBoolean(Integer i)
        {
            if (i == null)
                return false;
            return (i.intValue() == 1);
        }
    }

    /**
     * Creates a List of Key populated from the ResultSet.
     */
    private static final class KeyMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            KeyDesc keyDesc = new KeyDesc();
            keyDesc.keyId = rs.getString("key_id");
            keyDesc.fromTable = rs.getString("from_table");
            keyDesc.targetTable = rs.getString("target_table");
            keyDesc.description = rs.getString("description");
            keyDesc.utype = rs.getString("utype");
            keyDesc.keyColumnDescs = new ArrayList<KeyColumnDesc>();
            //log.debug("found: " + keyDesc);
            return keyDesc;
        }
    }

    /**
     * Creates a List of KeyColumn populated from the ResultSet.
     */
    private static final class KeyColumnMapper implements RowMapper
    {
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            KeyColumnDesc keyColumnDesc = new KeyColumnDesc();
            keyColumnDesc.keyId = rs.getString("key_id");
            keyColumnDesc.fromColumn = rs.getString("from_column");
            keyColumnDesc.targetColumn = rs.getString("target_column");
            //log.debug("found: " + keyColumnDesc);
            return keyColumnDesc;
        }
    }

}
