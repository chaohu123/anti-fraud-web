/** 与 Java `Result<T>` 一致，供前端 Axios 拦截器解包 */
export function ok<T>(data: T) {
  return { success: true, code: 200, message: '成功', data };
}

export function fail(code: number, message: string) {
  return { success: false, code, message, data: null };
}
