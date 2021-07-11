/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.datasource.jndi;

import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceException;
import org.apache.ibatis.datasource.DataSourceFactory;

/**
 * @author Clinton Begin
 *
 * 这个数据源的实现是为了能在如 EJB 或应用服务器这类容器中使用，
 * 容器可以集中或在外部配置数据源，然后放置一个 JNDI 上下文的引用。
 * 这种数据源配置只需要两个属性：
 */
public class JndiDataSourceFactory implements DataSourceFactory {

  /**
   * 这个属性用来在 InitialContext 中寻找上下文（即，initialContext.lookup(initial_context)）。
   * 这是个可选属性，如果忽略，那么 data_source 属性将会直接从 InitialContext 中寻找。
   */
  public static final String INITIAL_CONTEXT = "initial_context";

  /**
   * 这是引用数据源实例位置的上下文的路径。
   * 提供了 initial_context 配置时会在其返回的上下文中进行查找，
   * 没有提供时则直接在 InitialContext 中查找。
   */
  public static final String DATA_SOURCE = "data_source";
  public static final String ENV_PREFIX = "env.";

  private DataSource dataSource;

  @Override
  public void setProperties(Properties properties) {
    try {
      InitialContext initCtx;
      Properties env = getEnvProperties(properties);    // <1> 获得系统 Properties 对象
      if (env == null) {
        initCtx = new InitialContext();  // 创建 InitialContext 对象
      } else {
        initCtx = new InitialContext(env);
      }

      // 从 InitialContext 上下文中，获取 DataSource 对象
      if (properties.containsKey(INITIAL_CONTEXT) && properties.containsKey(DATA_SOURCE)) {
        Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
        dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
      } else if (properties.containsKey(DATA_SOURCE)) {
        dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
      }

    } catch (NamingException e) {
      throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
    }
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  private static Properties getEnvProperties(Properties allProps) {
    final String PREFIX = ENV_PREFIX;
    Properties contextProperties = null;
    for (Entry<Object, Object> entry : allProps.entrySet()) {
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (key.startsWith(PREFIX)) {
        if (contextProperties == null) {
          contextProperties = new Properties();
        }
        contextProperties.put(key.substring(PREFIX.length()), value);
      }
    }
    return contextProperties;
  }

}
