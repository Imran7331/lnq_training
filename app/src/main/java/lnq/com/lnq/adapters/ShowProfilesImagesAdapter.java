package lnq.com.lnq.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lnq.com.lnq.R;
import lnq.com.lnq.application.LnqApplication;
import lnq.com.lnq.common.Constants;
import lnq.com.lnq.model.ShowProfileImagesModel;
import lnq.com.lnq.model.event_bus_models.EventBusLoadImage;
import lnq.com.lnq.model.gson_converter_models.Contacts.connections.UserConnectionsData;
import lnq.com.lnq.utils.FontUtils;

import static lnq.com.lnq.fragments.profile.ProgressDialogFragmentImageCrop.TAG;

public class ShowProfilesImagesAdapter extends RecyclerView.Adapter<ShowProfilesImagesAdapter.UserExportContactsViewHolder> {
    private String cachePath = "";
    private TransferUtility transferUtility;

    //    Android fields....
    private Context context;
    private LayoutInflater layoutInflater;

    //    Instance fields....
    private List<ShowProfileImagesModel> exportContactsFilteredList = new ArrayList<>();
    private ImageView previousImageView;

    public ShowProfilesImagesAdapter(Context context, List<ShowProfileImagesModel> userConnectionsData) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        createTransferUtility();
        this.exportContactsFilteredList = userConnectionsData;
        cachePath = context.getCacheDir().getAbsolutePath();
    }

    private void createTransferUtility() {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context.getApplicationContext(),
                Constants.COGNITO_POOL_ID,
                Regions.US_WEST_1
        );
        AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(s3Client, context.getApplicationContext());
    }

    @NonNull
    @Override
    public UserExportContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.cus_profiles_images_dialog, parent, false);
        return new UserExportContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserExportContactsViewHolder holder, int position) {
        ShowProfileImagesModel userConnectionsData = exportContactsFilteredList.get(position);
        if (userConnectionsData.getImagePath() != null && !userConnectionsData.getImagePath().isEmpty()) {
            download(userConnectionsData.getImagePath(), holder.imageViewProfile);
            if(userConnectionsData.isSelected()){
                holder.imageViewProfileImagesTick.setVisibility(View.VISIBLE);
            }else{
                holder.imageViewProfileImagesTick.setVisibility(View.GONE);
            }
        }
    }


    @Override
    public int getItemCount() {
        return exportContactsFilteredList.size();
    }

    void download(String objectKey, ImageView imageView) {

        if (LnqApplication.getInstance().listImagePaths.contains(cachePath + "/" + objectKey)) {
            Glide.with(context).
                    load(BitmapFactory.decodeFile(cachePath + "/" + objectKey)).
                    apply(new RequestOptions().placeholder(R.drawable.ic_action_avatar))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } else {
            final File fileDownload = new File(cachePath, objectKey);

            TransferObserver transferObserver = transferUtility.download(
                    Constants.BUCKET_NAME,
                    objectKey,
                    fileDownload
            );
            transferObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    Log.d(TAG, "onStateChanged: " + state);
                    if (TransferState.COMPLETED.equals(state)) {
                        LnqApplication.getInstance().listImagePaths.add(fileDownload.getAbsolutePath());
                        Glide.with(context).
                                load(BitmapFactory.decodeFile(fileDownload.getAbsolutePath())).
                                apply(new RequestOptions().placeholder(R.drawable.ic_action_avatar))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageView);
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG, "onError: ", ex);
                }
            });
        }
    }

    class UserExportContactsViewHolder extends RecyclerView.ViewHolder {

        ImageView imageViewProfile, imageViewProfileImagesTick;

        UserExportContactsViewHolder(View itemView) {
            super(itemView);

            imageViewProfile = itemView.findViewById(R.id.imageViewProfileImages);
            imageViewProfileImagesTick = itemView.findViewById(R.id.imageViewProfileImagesTick);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EventBus.getDefault().post(new EventBusLoadImage(getBindingAdapterPosition()));
                }
            });
        }
    }
}
