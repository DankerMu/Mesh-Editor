import type { AxiosRequestConfig } from "axios";
import axios from "axios";
// import { ElMessage } from "element-plus";
import { setupAxiosInstance } from "./shared";

const noTokenPrefix = ['/login', '/verifyCode'];

// axios.defaults.headers.post["Content-Type"] = "application/x-www-form-urlencoded";
// axios.defaults.headers.withCredentials = true;
// axios.defaults.withCredentials = true;
function createAxiosInstance(createConfig: AxiosRequestConfig) {
  const instance = axios.create(createConfig);

  instance.interceptors.request.use((config) => {
    const url = config.url || '';
    const isWhitelisted = noTokenPrefix.some(prefix => url.startsWith(prefix));
    if (!isWhitelisted) {
      const tokenObj: string | null = localStorage.getItem("token")
      let token = ""
      if (tokenObj) {
        token = JSON.parse(tokenObj).token
      }
      if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
      }
    }

    const requestMethod = config.method?.toLowerCase();
    if (requestMethod === "post") {
      if (globalThis.FormData && config.data instanceof FormData) {
        config.headers.set("Content-Type", "multipart/form-data");
      } else {
        config.headers.set("Content-Type", "application/json");
        if (config.data) {
          if (!Array.isArray(config.data) && typeof config.data !== 'number') {
            config.data = {
              ...config.data,
            };
          }
        }
      }
    } else {
      config.params = {
        ...config.params
      };
    }

    return config;
  });

  instance.interceptors.response.use(
    async (response) => {
      if (response.request.responseType === "blob") {
        return response.data;
      }
      const { data } = response;

      const code = data.code ? Number(data.code) : null;
      if (code === 1003 || code === 401) {
        // ElMessage({
        //   type: "error",
        //   message: "token已过期，请重新登录！",
        //   duration: 1500
        // });
        // localStorage.removeItem("app.auth.token");
        // sessionStorage.removeItem("userInfo");
        // location.reload();
      } else if (code !== 1001 && code !== 200 && code) {
        // ElMessage({
        //   type: code === 1002 ? "warning" : "error",
        //   message: data.message || data.msg,
        //   duration: 2000
        // });
        // throw new Error(data.message);
      }
      return data;
    },
    (error) => {
      if (error.response) {
        const statusCode = Number(error.response.status);
        if (statusCode === 401 || statusCode === 403) {
          // ElMessage({
          //   type: "error",
          //   message: "token已过期，请重新登录！",
          //   duration: 1500
          // });
          // sessionStorage.removeItem("userInfo");
          // location.reload();
        }
        return Promise.reject(error);
      }

      // ElMessage({
      //   type: "error",
      //   message: "服务异常,请刷新重试!",
      //   duration: 1000
      // });

      return Promise.reject(error);
    }
  );

  return instance;
}

// 初始化公共实例
setupAxiosInstance(createAxiosInstance({
  timeout: 600 * 1000,
  baseURL: '/cdsz',
  // withCredentials: true
}));

