import { NativeModules } from 'react-native';

interface config {
  upiId: string;
  name: string;
  note: string;
  amount: string;
  targetPackage?: string;
  chooserText?: string;
}

interface success {
  status: 'SUCCESS';
  txnId: string;
  code: string;
  approvalRefNo: string;
}
interface error {
  status: 'FAILED';
  message: string;
}

const LINKING_ERROR =
  `The package 'one-react-native-upi' doesn't seem to be linked. Make sure: \n\n` +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const OneReactNativeUpi = NativeModules.OneReactNativeUpi
  ? NativeModules.OneReactNativeUpi
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const OneUpi = {
  initiate(
    { targetPackage = '', chooserText = 'Pay with ', ...config }: config,
    onSuccess: () => success,
    onFailure: () => error
  ) {
    OneReactNativeUpi.initiatePayment(
      { targetPackage, chooserText, ...config },
      onSuccess,
      onFailure
    );
  },

  getInstalledApps(): () => String[] {
    return OneReactNativeUpi.getInstalledUPIApps();
  },
};
export default OneUpi;
