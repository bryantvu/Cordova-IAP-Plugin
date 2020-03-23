package cordovaPluginIap;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.support.api.client.Status;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;


/**
 * This class echoes a string called from JavaScript.
 */
public class IapWrapper extends CordovaPlugin {

    private static final String NC_PRODUCTID = "NCProduct01";
    private static final String S_PRODUCTID = "SProduct01";

    private IapClient mClient;

    private List<ProductInfo> nonconsumableProducts = new ArrayList<>();

    private Boolean purchaseStatus = false;
    private String purchaseToken;
    private static CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Context context = this.cordova.getActivity().getApplicationContext();
        mClient = Iap.getIapClient(context);

        this.callbackContext = callbackContext;
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message);
            return true;
        }else if(action.equals("buyNonConsumable")){
//            callbackContext.success("execute >> buyNonConsumable");
//            String message = args.getString(0);
            this.buyNonConsumable();
            return true;
        }else if(action.equals("getNonConsumables")){
//            callbackContext.success("execute >> buyNonConsumable");
//            String message = args.getString(0);
            this.getNonConsumables();
            return true;
        }else if(action.equals("getSubscriptions")){
//            callbackContext.success("execute >> buyNonConsumable");
//            String message = args.getString(0);
            this.getSubscriptions();
            return true;
        }
        callbackContext.error("execute >> method does not exist");
        return false;
    }

    private void coolMethod(String message) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void buyNonConsumable() {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    queryPurchases();
//                    callbackContext.success("buyNonConsumable >> Success >> queryPurchases");
                } catch (Exception e) {
                    callbackContext.error("buyNonConsumable >> Error >> "+e.getMessage());
                }
            }
        });
//        if (message != null && message.length() > 0) {
//            callbackContext.success("buyNonConsumable >> Success >> congrats you did nothing "+ message);
//        } else {
//            callbackContext.error("Expected one non-empty string argument.");
//        }

    }

    private void queryPurchases() {

        try{
            // Query users' purchased non-consumable products.
            obtainOwnedPurchases(mClient, IapClient.PriceType.IN_APP_NONCONSUMABLE, new QueryPurchasesCallback() {
                @Override
                public void onSuccess(OwnedPurchasesResult result) {
//                Log.i(TAG, "obtainOwnedPurchases, success");
                    checkPurchaseState(result);
                    callbackContext.success("queryPurchases >> success >> checkPurchaseState");
                }

                @Override
                public void onFail(Exception e) {
//                Log.e(TAG, "obtainOwnedPurchases, type=" + IapClient.PriceType.IN_APP_NONCONSUMABLE + ", " + e.getMessage());
//                showError();
                    callbackContext.success("queryPurchases >> fail >> "+ e.toString());
//                Utils.showMessage(NonConsumptionActivity.this, "get Purchases fail, " + e.getMessage());
                }
            });
        }catch(Exception e){
            callbackContext.error("queryPurchases >> Error >> "+e.getMessage());
        }
    }

    public void obtainOwnedPurchases(IapClient mClient, final int type, final QueryPurchasesCallback callback) {
//        Log.i(TAG, "call obtainOwnedPurchases");
        String continuationToken = null;
        Task<OwnedPurchasesResult> task = mClient.obtainOwnedPurchases(createOwnedPurchasesReq(type, continuationToken));
        task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
//                Log.i(TAG, "obtainOwnedPurchases success");
                callback.onSuccess(result);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
//                Log.e(TAG, "obtainOwnedPurchases fail");
                callback.onFail(e);
            }
        });

    }

    private OwnedPurchasesReq createOwnedPurchasesReq(int type, String continuationToken) {
        OwnedPurchasesReq req = new OwnedPurchasesReq();
        req.setPriceType(type);
        req.setContinuationToken(continuationToken);
        return req;
    }

    private void checkPurchaseState(OwnedPurchasesResult result) {
        if (result == null || result.getInAppPurchaseDataList() == null) {
//            Log.d(TAG, "checkPurchaseState >> result is null");
            return;
        }
//        Log.d(TAG, "checkPurchaseState >> result is valid");
        List<String> inAppPurchaseDataList = result.getInAppPurchaseDataList();
        List<String> inAppSignature= result.getInAppSignature();

        for (int i = 0; i < inAppPurchaseDataList.size(); i++) {
            try{
                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseDataList.get(i));
                if (NC_PRODUCTID.equals(inAppPurchaseDataBean.getProductId())) {
//                    Log.d(TAG, "checkPurchaseState >> NC Product Found >> " + inAppPurchaseDataList.get(i));
                    purchaseToken = inAppPurchaseDataBean.getPurchaseToken();
                    purchaseStatus = true;
                    break;
                }
            }catch(Exception e){

            }
//            Log.d(TAG, "checkPurchaseState >> "+inAppPurchaseDataList.get(i));
        }
//        if (purchaseStatus) {
//            showPurchaseStatus();
//        }
    }

    private void getNonConsumables() {
//        Log.d(TAG, "getProducts");
        nonconsumableProducts.clear();
        List<String> productIds = new ArrayList<>();
        productIds.add(NC_PRODUCTID);
        obtainProductInfo(mClient, productIds, IapClient.PriceType.IN_APP_NONCONSUMABLE, new ProductInfoCallback() {
            @Override
            public void onSuccess(ProductInfoResult result) {
//                Log.i(TAG, "obtainProductInfo, success");
                if (result == null || result.getProductInfoList() == null) {
                    callbackContext.error("getNonConsumables >> error >> result is null");
                    return;
                }
                // to show product information
                ProductInfo tempProductInfo = null;
                for (ProductInfo productInfo : result.getProductInfoList()) {
                    if (productInfo != null && NC_PRODUCTID.equals(productInfo.getProductId())) {
                        tempProductInfo = productInfo;
                        callbackContext.success("getNonConsumables >> success >> product >> "
                                +tempProductInfo.getProductName() + " = " + tempProductInfo.getPrice());
                        break;
                    }
                }
//                if (tempProductInfo != null) {
//                    showProducts(tempProductInfo);
//                }
            }

            @Override
            public void onFail(Exception e) {
                callbackContext.error("getNonConsumables >> error >> "+e.getMessage());
            }
        });
    }

    public static void obtainProductInfo(IapClient iapClient, final List<String> productIds, int type, final ProductInfoCallback callback) {
//        Log.i(TAG, "call obtainProductInfo");

        Task<ProductInfoResult> task = iapClient.obtainProductInfo(createProductInfoReq(type, productIds));
        task.addOnSuccessListener(new OnSuccessListener<ProductInfoResult>() {
            @Override
            public void onSuccess(ProductInfoResult result) {
//                Log.i(TAG, "obtainProductInfo, success");
                callback.onSuccess(result);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
//                Log.e(TAG, "obtainProductInfo, fail");
//                callback.onFail(e);
                callbackContext.error("obtainProductInfo >> Error >> "+e.getMessage());
            }
        });
    }

    private static ProductInfoReq createProductInfoReq(int type, List<String> productIds) {
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(type);
        req.setProductIds(productIds);
        return req;
    }

    private interface QueryPurchasesCallback {
        void onSuccess(OwnedPurchasesResult result);

        void onFail(Exception e);
    }

    private interface ProductInfoCallback {

        void onSuccess(ProductInfoResult result);

        void onFail(Exception e);
    }

    private void getSubscriptions() {
        System.out.println("getSubscriptions");
        List<String> productIds = new ArrayList<>();
        productIds.add(S_PRODUCTID);
//        Log.e(TAG, "queryProducts: "+productIds.toString());
        obtainProductInfo(mClient, productIds, IapClient.PriceType.IN_APP_SUBSCRIPTION, new ProductInfoCallback() {
            @Override
            public void onSuccess(final ProductInfoResult result) {
                System.out.println("getSubscriptions >> onSuccess");
                if (null == result) {
//                    Log.e(TAG, "ProductInfoResult is null");
                    return;
                }

                List<ProductInfo> productInfos = result.getProductInfoList();

                JSONArray response = new JSONArray();
                productInfos.forEach((skuDetails) ->{
                    if (skuDetails != null) {
                        JSONObject detailsJson = new JSONObject();
                        try {
                            detailsJson.put("productId", skuDetails.getProductId());
                            detailsJson.put("title", skuDetails.getProductName());
                            detailsJson.put("description", skuDetails.getProductDesc());
                            //detailsJson.put("currency", skuDetails.getPriceCurrencyCode());
                            detailsJson.put("price", skuDetails.getPrice());
                            detailsJson.put("type", skuDetails.getPriceType());
                            response.put(detailsJson);
//                            Log.d(TAG, "queryProducts >> "+response);
//                            callback.onSuccess(detailsJson);
                            System.out.println("getSubscriptions >> onSuccess >> " + detailsJson);
                        } catch (JSONException e) {
//                            callbackContextPublic.error(e.getMessage());
                            callbackContext.error("getSubscriptions >> Error >> "+e.getMessage());
                        }
                    }
                });
//                callbackContextPublic.success(response);
//                view.showProducts(productInfos);
            }

            @Override
            public void onFail(Exception e) {
                callbackContext.error("getSubscriptions >> Error >> "+e.getMessage());

            }
        });
    }
}


