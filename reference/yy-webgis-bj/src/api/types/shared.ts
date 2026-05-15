export interface PagingDataResponse<T> {
  count: number;
  data: T[];
  rows?: T[];
  success: boolean;
  code: number;
}

export type Data = Record<string, any>;

export interface Sort {
  filedName: string;
  status: string | number;
}

export interface HttpResponse<T = unknown> {
  message: string;
  code: string;
  data: T;
  success: boolean;
  count: number;
}

export interface CodeAndName {
  code: string;
  name: string;
}
