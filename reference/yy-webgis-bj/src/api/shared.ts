import axios from "axios";
import type { AxiosInstance, AxiosRequestConfig } from "axios";
import type { HttpResponse } from "./types";

export interface RequestInstance {
  (url: string, data: any, options?: RequestOptions): Promise<
    HttpResponse<unknown>
  >;
  get<T = any>(url: string, config?: RequestOptions): Promise<T>;
  post<T = any>(url: string, data?: any, config?: RequestOptions): Promise<T>;
  put<T = any>(url: string, data?: any, config?: RequestOptions): Promise<T>;
  delete<T = any>(url: string, data?: any, config?: RequestOptions): Promise<T>;
}

export type DefineRequestModule<T> = (request: RequestInstance) => T;
export interface RequestOptions extends AxiosRequestConfig {}

export type RequestFunction = <T = HttpResponse<unknown>, D = any>(
  url: string,
  data: D,
  options?: RequestOptions
) => Promise<T>;

let axiosBase: AxiosInstance | null = null;

// 初始化公共实例
export function setupAxiosInstance(instance?: AxiosInstance) {
  axiosBase = instance ?? axiosBase ?? axios.create();
  return axiosBase;
}

// 声明请求模块
export function defineRequest<T>(fn: DefineRequestModule<T>): T {
  return fn(setupAxiosInstance());
}

// mock服务地址
// export const MOCK_SERVICE = "/mockservice";

/**
 * 服务地址
 */
// export const API_SERVICE =
//   process.env.NODE_ENV === "production"
//     ? import.meta.env.APP_API_URL
//     : import.meta.env.APP_API_DEV_URL;
