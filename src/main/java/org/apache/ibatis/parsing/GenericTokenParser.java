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
package org.apache.ibatis.parsing;

/**
 * 通用的 Token 解析器
 */
public class GenericTokenParser {

  // 开始的 Token 字符串
  private final String openToken;
  // 结束的 Token 字符串
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }

    // search open token
    // 寻找开始的 openToken 的位置
    int start = text.indexOf(openToken);
    if (start == -1) {   // // 找不到，直接返回
      return text;
    }
    char[] src = text.toCharArray();
    int offset = 0;  // 起始查找位置
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;  //// 匹配到 openToken 和 closeToken 之间的表达式

    // 循环匹配
    do {
      if (start > 0 && src[start - 1] == '\\') {   // 转义字符

        // 因为 openToken 前面一个位置是 \ 转义字符，所以忽略 \
        // 添加 [offset, start - offset - 1] 和 openToken 的内容，添加到 builder 中
        builder.append(src, offset, start - offset - 1).append(openToken);

        // 修改 offset
        offset = start + openToken.length();
      } else {     // 非转义字符
        // found open token. let's search close token.
        // 创建/重置 expression 对象
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }

        // 添加 offset 和 openToken 之间的内容，添加到 builder 中
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();     // 修改 offset
        int end = text.indexOf(closeToken, offset);    // 寻找结束的 closeToken 的位置
        while (end > -1) {
          if (end > offset && src[end - 1] == '\\') {   // 转义

            // this close token is escaped. remove the backslash and continue.
            // 因为 endToken 前面一个位置是 \ 转义字符，所以忽略 \
            // 添加 [offset, end - offset - 1] 和 endToken 的内容，添加到 builder 中
            expression.append(src, offset, end - offset - 1).append(closeToken);


            offset = end + closeToken.length();
            end = text.indexOf(closeToken, offset);  // 继续，寻找结束的 closeToken 的位置
          } else {
            expression.append(src, offset, end - offset); // 添加 [offset, end - offset] 的内容，添加到 builder 中
            break;
          }
        }
        if (end == -1) {
          // close token was not found.
          builder.append(src, start, src.length - start);    // closeToken 未找到，直接拼接
          offset = src.length;
        } else {

          // closeToken 找到，将 expression 提交给 handler 处理 ，并将处理结果添加到 builder 中
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }

      // 继续，寻找开始的 openToken 的位置
      start = text.indexOf(openToken, offset);
    } while (start > -1);


    if (offset < src.length) {  // 拼接剩余的部分
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
