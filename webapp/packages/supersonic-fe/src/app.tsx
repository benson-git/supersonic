import { AUTH_TOKEN_KEY, FROM_URL_KEY } from '@/common/constants';
import RightContent from '@/components/RightContent';
import S2Icon, { ICON } from '@/components/S2Icon';
import type { Settings as LayoutSettings } from '@ant-design/pro-layout';
import { Space, Spin } from 'antd';
import qs from 'qs';
import ScaleLoader from 'react-spinners/ScaleLoader';
import type { RunTimeLayoutConfig } from 'umi';
import { history } from 'umi';
import defaultSettings from '../config/defaultSettings';
import settings from '../config/themeSettings';
import { queryToken } from './services/login';
import { queryCurrentUser } from './services/user';
import { traverseRoutes, deleteUrlQuery } from './utils/utils';
import { publicPath } from '../config/defaultSettings';
export { request } from './services/request';

const TOKEN_KEY = AUTH_TOKEN_KEY;

const replaceRoute = '/';

const getRuningEnv = async () => {
  try {
    const response = await fetch(`${publicPath}supersonic.config.json`);
    const config = await response.json();
    return config;
  } catch (error) {
    console.warn('无法获取配置文件: 运行时环境将以semantic启动');
  }
};

Spin.setDefaultIndicator(
  <ScaleLoader color={settings['primary-color']} height={25} width={2} radius={2} margin={2} />,
);

export const initialStateConfig = {
  loading: (
    <Spin wrapperClassName="initialLoading">
      <div className="loadingPlaceholder" />
    </Spin>
  ),
};

const getToken = async () => {
  let { search } = window.location;
  if (search.length > 0) {
    search = search.slice(1);
  }
  const data = qs.parse(search);
  if (data.code) {
    try {
      const fromUrl = localStorage.getItem(FROM_URL_KEY);
      const res = await queryToken(data.code as string);
      localStorage.setItem(TOKEN_KEY, res.payload);
      const newUrl = deleteUrlQuery(window.location.href, 'code');
      window.location.href = fromUrl || newUrl;
    } catch (err) {
      console.log(err);
    }
  }
};

const getAuthCodes = () => {
  return [];
};

export async function getInitialState(): Promise<{
  settings?: LayoutSettings;
  currentUser?: API.CurrentUser;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
  codeList?: string[];
  authCodes?: string[];
}> {
  // await getRuningEnv();
  const fetchUserInfo = async () => {
    try {
      const { code, data } = await queryCurrentUser();
      if (code === 200) {
        return { ...data, staffName: data.staffName || data.name };
      }
    } catch (error) {}
    return undefined;
  };
  const { query } = history.location as any;
  const currentToken = query[TOKEN_KEY] || localStorage.getItem(TOKEN_KEY);

  if (window.location.host.includes('tmeoa') && !currentToken) {
    await getToken();
  }

  let currentUser: any;
  if (!window.location.pathname.includes('login')) {
    currentUser = await fetchUserInfo();
  }

  if (currentUser) {
    localStorage.setItem('user', currentUser.staffName);
    if (currentUser.orgName) {
      localStorage.setItem('organization', currentUser.orgName);
    }
  }

  const authCodes = getAuthCodes();

  return {
    fetchUserInfo,
    currentUser,
    settings: defaultSettings,
    authCodes,
  };
}

export async function patchRoutes({ routes }) {
  const config = await getRuningEnv();
  if (config && config.env) {
    const { env } = config;
    const target = routes[0].routes;
    if (env) {
      const envRoutes = traverseRoutes(target, env);
      // 清空原本route;
      target.splice(0, 99);
      // 写入根据环境转换过的的route
      target.push(...envRoutes);
    }
  }
}

export const layout: RunTimeLayoutConfig = (params) => {
  const { initialState } = params as any;
  return {
    onMenuHeaderClick: (e) => {
      e.preventDefault();
      history.push(replaceRoute);
    },
    logo: (
      <Space>
        <S2Icon
          icon={ICON.iconlogobiaoshi}
          size={30}
          color="#fff"
          style={{ display: 'inline-block', marginTop: 8 }}
        />
        <div className="logo">超音数(SuperSonic)</div>
      </Space>
    ),
    contentStyle: { ...(initialState?.contentStyle || {}) },
    rightContentRender: () => <RightContent />,
    disableContentMargin: true,
    menuHeaderRender: undefined,
    childrenRender: (dom) => {
      return dom;
    },
    openKeys: false,
    ...initialState?.settings,
  };
};
