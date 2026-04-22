package org.springframework.boot.jta.narayana;

import com.arjuna.ats.jta.TransactionManager;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

public class NarayanaXADataSourceWrapper {

    public DataSource wrapDataSource(XADataSource xaDataSource) {
        Objects.requireNonNull(xaDataSource);
        return new WrappedXADataSource(xaDataSource);
    }

    private static class WrappedXADataSource implements DataSource {
        private final XADataSource xa;

        WrappedXADataSource(XADataSource xa) {
            this.xa = xa;
        }

        @Override
        public Connection getConnection() throws SQLException {
            try {
                XAConnection xac = xa.getXAConnection();
                Connection conn = xac.getConnection();
                enlistIfPossible(xac);
                return proxyConnection(conn, xac);
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                throw new SQLException("Failed to get XA connection", e);
            }
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            try {
                XAConnection xac = xa.getXAConnection(username, password);
                Connection conn = xac.getConnection();
                enlistIfPossible(xac);
                return proxyConnection(conn, xac);
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                throw new SQLException("Failed to get XA connection", e);
            }
        }

        private void enlistIfPossible(XAConnection xac) {
            try {
                Object xaRes = null;
                try {
                    Method getXa = xac.getClass().getMethod("getXAResource");
                    xaRes = getXa.invoke(xac);
                } catch (Exception e) {
                    try {
                        xaRes = xac.getXAResource();
                    } catch (Throwable ignored) {
                    }
                }

                Object jtx = TransactionManager.transactionManager().getTransaction();
                if (jtx != null && xaRes != null) {
                    Method enlist = null;
                    for (Method m : jtx.getClass().getMethods()) {
                        if (m.getName().equals("enlistResource") && m.getParameterCount() == 1) {
                            enlist = m;
                            break;
                        }
                    }
                    if (enlist != null) {
                        try {
                            enlist.invoke(jtx, xaRes);
                        } catch (IllegalArgumentException iae) {
                            for (Method m : jtx.getClass().getMethods()) {
                                if (m.getName().equals("enlistResource") && m.getParameterCount() == 1) {
                                    m.invoke(jtx, xaRes);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                try {
                    Logger.getLogger(NarayanaXADataSourceWrapper.class.getName()).fine("Failed to enlist XA resource: " + t.getMessage());
                } catch (Throwable ignored) {}
            }
        }

        private Connection proxyConnection(Connection conn, XAConnection xac) {
            return (Connection) Proxy.newProxyInstance(
                    conn.getClass().getClassLoader(),
                    new Class[]{Connection.class},
                    new ConnectionInvocationHandler(conn, xac));
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override
        public PrintWriter getLogWriter() throws SQLException { return null; }
        @Override
        public void setLogWriter(PrintWriter out) throws SQLException { }
        @Override
        public void setLoginTimeout(int seconds) throws SQLException { }
        @Override
        public int getLoginTimeout() throws SQLException { return 0; }
        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
    }

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection delegate;
        private final XAConnection xaConnection;

        ConnectionInvocationHandler(Connection delegate, XAConnection xaConnection) {
            this.delegate = delegate;
            this.xaConnection = xaConnection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("close".equals(name)) {
                try {
                    Object res = method.invoke(delegate, args);
                    try { xaConnection.close(); } catch (Exception ignored) {}
                    return res;
                } catch (Throwable t) {
                    try { xaConnection.close(); } catch (Exception ignored) {}
                    throw t;
                }
            }
            return method.invoke(delegate, args);
        }
    }
}
