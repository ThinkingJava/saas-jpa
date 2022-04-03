package com.multi.tenant.multitenancy;

import com.multi.tenant.task.SpringContextHolder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.Objects;


@Slf4j
@Data
@Accessors(chain = true)
public class TenantInterceptor implements StatementInspector {

    private TenantLineHandler tenantLineHandler;

    /**
     * 重写StatementInspector的inspect接口，参数为hibernate处理后的原始SQL，返回值为我们修改后的SQL
     *
     * @param sql
     * @return
     */
    @Override
    public String inspect(String sql) {
        try {
            if (Objects.isNull(tenantLineHandler)) {
                tenantLineHandler = SpringContextHolder.getBean(TenantLineHandler.class);
            }
            log.info("租户解析开始，原始SQL：{}", sql);
            StringBuilder sb = new StringBuilder();
            Statements statements = CCJSqlParserUtil.parseStatements(sql);

            int i = 0;
            for (Statement statement : statements.getStatements()) {
                if (i++ > 0) {
                    sb.append(";");
                }
                sb.append(processParser(statement, i, sql, null));
                i++;
            }
            String newSql = sb.toString();
            log.info("租户解析结束，解析后SQL：{}", newSql);
            return sb.toString();
        } catch (Exception e) {
            log.error("租户解析失败，解析SQL异常{}", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 执行 SQL 解析
     *
     * @param statement JsqlParser Statement
     * @return sql
     */
    protected String processParser(Statement statement, int index, String sql, Object obj) {
        if (log.isDebugEnabled()) {
            log.debug("SQL to parse, SQL: " + sql);
        }
        if (statement instanceof Insert) {
            this.processInsert((Insert) statement, index, sql, obj);
        } else if (statement instanceof Select) {
            this.processSelect((Select) statement, index, sql, obj);
        } else if (statement instanceof Update) {
            this.processUpdate((Update) statement, index, sql, obj);
        } else if (statement instanceof Delete) {
            this.processDelete((Delete) statement, index, sql, obj);
        }
        sql = statement.toString();
        /**
         * 返回处理后的SQL
         */
        return sql;
    }

    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        if (tenantLineHandler.ignoreTable(insert.getTable().getName())) {
            // 过滤退出执行
            return;
        }
        List<Column> columns = insert.getColumns();
        if (Objects.isNull(columns) || columns.isEmpty()) {
            // 针对不给列名的insert 不处理
            return;
        }
        String tenantIdColumn = tenantLineHandler.getTenantIdColumn();
        if (columns.stream().map(Column::getColumnName).anyMatch(i -> i.equals(tenantIdColumn))) {
            // 针对已给出租户列的insert 不处理
            return;
        }
        columns.add(new Column(tenantLineHandler.getTenantIdColumn()));
        Select select = insert.getSelect();
        if (select != null) {
            this.processInsertSelect(select.getSelectBody());
        } else if (insert.getItemsList() != null) {
            ItemsList itemsList = insert.getItemsList();
            if (itemsList instanceof MultiExpressionList) {
                ((MultiExpressionList) itemsList).getExprList().forEach(el -> el.getExpressions().add(tenantLineHandler.getTenantId()));
            } else {
                ((ExpressionList) itemsList).getExpressions().add(tenantLineHandler.getTenantId());
            }
        } else {
            throw new RuntimeException("Failed to process multiple-table update, please exclude the tableName or statementId");
        }
    }

    /**
     * select 语句处理
     */
    protected void processSelect(Select select, int index, String sql, Object obj) {
        processSelectBody(select.getSelectBody());
        List<WithItem> withItemsList = select.getWithItemsList();
        if (!(Objects.isNull(withItemsList) || withItemsList.isEmpty())) {
            withItemsList.forEach(this::processSelectBody);
        }
    }

    protected void processSelectBody(SelectBody selectBody) {
        if (selectBody == null) {
            return;
        }
        if (selectBody instanceof PlainSelect) {
            processPlainSelect((PlainSelect) selectBody);
        } else if (selectBody instanceof WithItem) {
            WithItem withItem = (WithItem) selectBody;
            processSelectBody(withItem.getSelectBody());
        } else {
            SetOperationList operationList = (SetOperationList) selectBody;
            if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {
                operationList.getSelects().forEach(this::processSelectBody);
            }
        }
    }

    /**
     * update 语句处理
     */

    protected void processUpdate(Update update, int index, String sql, Object obj) {
        final Table table = update.getTable();
        if (tenantLineHandler.ignoreTable(table.getName())) {
            // 过滤退出执行
            return;
        }
        List<Column> columns = update.getColumns();
        if (!columns.stream().map(Column::getColumnName).anyMatch(i -> i.equals("updateName"))) {
            // 针对已给出updateName 不处理
            columns.add(new Column("update_name"));
            update.getExpressions().add(new StringValue("aaaa"));
//            FromItem itemsList = update.getFromItem();
//            if (itemsList instanceof MultiExpressionList) {
//                ((MultiExpressionList) itemsList).getExprList().forEach(el -> el.getExpressions().add(new StringValue("test")));
//            } else {
//                ((ExpressionList) itemsList).getExpressions().add(new StringValue("test"));
//            }
        }

        update.setWhere(this.andExpression(table, update.getWhere()));
    }

    /**
     * delete 语句处理
     */

    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        if (tenantLineHandler.ignoreTable(delete.getTable().getName())) {
            // 过滤退出执行
            return;
        }
        delete.setWhere(this.andExpression(delete.getTable(), delete.getWhere()));
    }

    /**
     * delete update 语句 where 处理
     */
    protected BinaryExpression andExpression(Table table, Expression where) {
        //获得where条件表达式
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(this.getAliasColumn(table));
        equalsTo.setRightExpression(tenantLineHandler.getTenantId());
        if (null != where) {
            if (where instanceof OrExpression) {
                return new AndExpression(equalsTo, new Parenthesis(where));
            } else {
                return new AndExpression(equalsTo, where);
            }
        }
        return equalsTo;
    }


    /**
     * 处理 insert into select
     * <p>
     * 进入这里表示需要 insert 的表启用了多租户,则 select 的表都启动了
     *
     * @param selectBody SelectBody
     */
    protected void processInsertSelect(SelectBody selectBody) {
        PlainSelect plainSelect = (PlainSelect) selectBody;
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table) {
            Table fromTable = (Table) fromItem;
            plainSelect.setWhere(builderExpression(plainSelect.getWhere(), fromTable));
            appendSelectItem(plainSelect.getSelectItems());
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            appendSelectItem(plainSelect.getSelectItems());
            processInsertSelect(subSelect.getSelectBody());
        }
    }

    /**
     * 追加 SelectItem
     *
     * @param selectItems SelectItem
     */
    protected void appendSelectItem(List<SelectItem> selectItems) {
        if (Objects.isNull(selectItems) || selectItems.isEmpty()) return;
        if (selectItems.size() == 1) {
            SelectItem item = selectItems.get(0);
            if (item instanceof AllColumns || item instanceof AllTableColumns) return;
        }
        selectItems.add(new SelectExpressionItem(new Column(tenantLineHandler.getTenantIdColumn())));
    }

    /**
     * 处理 PlainSelect
     */
    protected void processPlainSelect(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        Expression where = plainSelect.getWhere();
        processWhereSubSelect(where);
        if (fromItem instanceof Table) {
            Table fromTable = (Table) fromItem;
            if (!tenantLineHandler.ignoreTable(fromTable.getName())) {
                //#1186 github
                plainSelect.setWhere(builderExpression(where, fromTable));
            }
        } else {
            processFromItem(fromItem);
        }
        List<Join> joins = plainSelect.getJoins();
        if (joins != null && joins.size() > 0) {
            joins.forEach(j -> {
                processJoin(j);
                processFromItem(j.getRightItem());
            });
        }
    }

    /**
     * 处理where条件内的子查询
     * <p>
     * 支持如下:
     * 1. in
     * 2. =
     * 3. >
     * 4. <
     * 5. >=
     * 6. <=
     * 7. <>
     * 8. EXISTS
     * 9. NOT EXISTS
     * <p>
     * 前提条件:
     * 1. 子查询必须放在小括号中
     * 2. 子查询一般放在比较操作符的右边
     *
     * @param where where 条件
     */
    protected void processWhereSubSelect(Expression where) {
        if (where == null) {
            return;
        }
        if (where instanceof FromItem) {
            processFromItem((FromItem) where);
            return;
        }
        if (where.toString().indexOf("SELECT") > 0) {
            // 有子查询
            if (where instanceof BinaryExpression) {
                // 比较符号 , and , or , 等等
                BinaryExpression expression = (BinaryExpression) where;
                processWhereSubSelect(expression.getLeftExpression());
                processWhereSubSelect(expression.getRightExpression());
            } else if (where instanceof InExpression) {
                // in
                InExpression expression = (InExpression) where;
                ItemsList itemsList = expression.getRightItemsList();
                if (itemsList instanceof SubSelect) {
                    processSelectBody(((SubSelect) itemsList).getSelectBody());
                }
            } else if (where instanceof ExistsExpression) {
                // exists
                ExistsExpression expression = (ExistsExpression) where;
                processWhereSubSelect(expression.getRightExpression());
            } else if (where instanceof NotExpression) {
                // not exists
                NotExpression expression = (NotExpression) where;
                processWhereSubSelect(expression.getExpression());
            } else if (where instanceof Parenthesis) {
                Parenthesis expression = (Parenthesis) where;
                processWhereSubSelect(expression.getExpression());
            }
        }
    }

    /**
     * 处理子查询等
     */
    protected void processFromItem(FromItem fromItem) {
        if (fromItem instanceof SubJoin) {
            SubJoin subJoin = (SubJoin) fromItem;
            if (subJoin.getJoinList() != null) {
                subJoin.getJoinList().forEach(this::processJoin);
            }
            if (subJoin.getLeft() != null) {
                processFromItem(subJoin.getLeft());
            }
        } else if (fromItem instanceof SubSelect) {
            SubSelect subSelect = (SubSelect) fromItem;
            if (subSelect.getSelectBody() != null) {
                processSelectBody(subSelect.getSelectBody());
            }
        } else if (fromItem instanceof ValuesList) {
            log.info("Perform a subquery, if you do not give us feedback");
        } else if (fromItem instanceof LateralSubSelect) {
            LateralSubSelect lateralSubSelect = (LateralSubSelect) fromItem;
            if (lateralSubSelect.getSubSelect() != null) {
                SubSelect subSelect = lateralSubSelect.getSubSelect();
                if (subSelect.getSelectBody() != null) {
                    processSelectBody(subSelect.getSelectBody());
                }
            }
        }
    }

    /**
     * 处理联接语句
     */
    protected void processJoin(Join join) {
        if (join.getRightItem() instanceof Table) {
            Table fromTable = (Table) join.getRightItem();
            if (tenantLineHandler.ignoreTable(fromTable.getName())) {
                // 过滤退出执行
                return;
            }
            join.setOnExpression(builderExpression(join.getOnExpression(), fromTable));
        }
    }

    /**
     * 处理条件
     */
    protected Expression builderExpression(Expression currentExpression, Table table) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(this.getAliasColumn(table));
        equalsTo.setRightExpression(tenantLineHandler.getTenantId());
        if (currentExpression == null) {
            return equalsTo;
        }
        if (currentExpression instanceof OrExpression) {
            return new AndExpression(new Parenthesis(currentExpression), equalsTo);
        } else {
            return new AndExpression(currentExpression, equalsTo);
        }
    }

    /**
     * 租户字段别名设置
     * <p>tenantId 或 tableAlias.tenantId</p>
     *
     * @param table 表对象
     * @return 字段
     */
    protected Column getAliasColumn(Table table) {
        StringBuilder column = new StringBuilder();
        if (table.getAlias() != null) {
            column.append(table.getAlias().getName()).append(".");
        }
        column.append(tenantLineHandler.getTenantIdColumn());
        return new Column(column.toString());
    }

}
