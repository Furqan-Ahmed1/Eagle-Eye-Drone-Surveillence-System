#To run the project simply run the track_objects.py with cmd: python track_objects.py 

# CPU
conda env create -f conda-cpu.yml

# GPU
conda env create -f conda-gpu.yml

# activate environment on Windows or Linux
conda activate tf-gpu

# CPU
pip install -r requirements.txt

# GPU
pip install -r requirements-gpu.txt

**Note:** If installing GPU version with Pip, you need to install CUDA and cuDNN in your system. You can find the tutorial for Windows [here](https://www.youtube.com/watch?v=PlW9zAg4cx8).

# Download Weights File
If using ``tiny`` version, download [yolov4-tiny.weights](https://github.com/AlexeyAB/darknet/releases/download/darknet_yolo_v4_pre/yolov4-tiny.weights) file instead. ``tiny`` version is faster, but less accurate.

# Convert YOLOv4 to TensorFlow

```bash
# Convert darknet weights to tensorflow
## yolov4
python save_model.py --weights ./data/yolov4.weights --output ./checkpoints/yolov4-416 --input_size 416 --model yolov4 

## yolov4-tiny
python save_model.py --weights ./data/yolov4-tiny.weights --output ./checkpoints/yolov4-tiny-416 --input_size 416 --model yolov4 --tiny

```
If you want to run yolov3 or yolov3-tiny change ``--model yolov3`` in command and also download corresponding YOLOv3 weights and and change ``--weights to ./data/yolov3.weights``

### Run Tracking

```bash
# Run Tracking on Video
python track_objects.py --weights ./checkpoints/yolov4-416 --score 0.3 --video ./data/dog.mp4 --output ./results/demo.avi --model yolov4

# Run Tracking on Webcam
python track_objects.py --weights ./checkpoints/yolov4-416 --score 0.3 --video 0 --output ./results/webcam.avi --model yolov4

# Run Tracking on Video With Tiny Yolov4
python track_objects.py --weights ./checkpoints/yolov4-tiny-416 --score 0.3 --video ./data/dog.mp4 --output ./results/demo_tiny.avi --model yolov4

# Run Tracking on Webcam With Tiny Yolov4
python track_objects.py --weights ./checkpoints/yolov4-tiny-416 --score 0.3 --video 0 --output ./results/webcam_tiny.avi --model yolov4

# Flags 

save_model.py:
  --weights: path to weights file
    (default: './data/yolov4.weights')
  --output: path to output
    (default: './checkpoints/yolov4-416')
  --[no]tiny: yolov4 or yolov4-tiny
    (default: 'False')
  --input_size: define input size of export model
    (default: 416)
  --framework: what framework to use (tf, trt, tflite)
    (default: tf)
  --model: yolov3 or yolov4
    (default: yolov4)
    
 object_tracker.py:
  --video: path to input video (use 0 for webcam & Drone)
    (default: './data/video/test.mp4')
  --output: path to output video (remember to set right codec for given format. e.g. XVID for .avi)
    (default: None)
  --output_format: codec used in VideoWriter when saving video to file
    (default: 'XVID)
  --[no]tiny: yolov4 or yolov4-tiny
    (default: 'false')
  --weights: path to weights file
    (default: './checkpoints/yolov4-416')
  --framework: what framework to use (tf, trt, tflite)
    (default: tf)
  --model: yolov3 or yolov4
    (default: yolov4)
  --size: resize images to
    (default: 416)
  --iou: iou threshold
    (default: 0.45)
  --score: confidence threshold
    (default: 0.50)
  --dont_show: dont show video output
    (default: False)
  --info: print detailed info about tracked objects
    (default: False)